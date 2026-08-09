package org.tavall.ai.agent.codex;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.tavall.ai.agent.AIAgentExecutionRequest;
import org.tavall.ai.agent.AIAgentExecutionResult;
import org.tavall.ai.agent.AIAgentExecutionStatus;
import org.tavall.ai.agent.AIAgentProvider;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/** Development-workspace Codex CLI provider. */
public final class CodexAgentProvider implements AIAgentProvider {
    public static final String PROVIDER_ID = "codex";
    private static final int MAX_CAPTURE_BYTES = 256 * 1024;
    private static final Set<String> INHERITED_ENVIRONMENT_ALLOWLIST = Set.of(
            "PATH", "PATHEXT", "SystemRoot", "SYSTEMROOT", "WINDIR", "COMSPEC",
            "HOME", "USERPROFILE", "TMP", "TEMP", "TMPDIR", "LANG", "LC_ALL", "TERM",
            "CODEX_HOME", "OPENAI_API_KEY", "OPENAI_BASE_URL", "OPENAI_ORG_ID", "OPENAI_PROJECT_ID",
            "SSL_CERT_FILE", "SSL_CERT_DIR"
    );

    private final CodexAgentProviderConfiguration configuration;
    private final CodexWorkspaceResolver workspaceResolver;
    private final ObjectMapper objectMapper;
    private final CodexCommandBuilder commandBuilder;

    public CodexAgentProvider(
            CodexAgentProviderConfiguration configuration,
            CodexWorkspaceResolver workspaceResolver,
            ObjectMapper objectMapper
    ) {
        this.configuration = Objects.requireNonNull(configuration, "configuration");
        this.workspaceResolver = Objects.requireNonNull(workspaceResolver, "workspaceResolver");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.commandBuilder = new CodexCommandBuilder(configuration);
    }

    @Override
    public String providerId() {
        return PROVIDER_ID;
    }

    @Override
    public AIAgentExecutionResult execute(AIAgentExecutionRequest request) throws Exception {
        AIAgentExecutionRequest safeRequest = Objects.requireNonNull(request, "request");
        validateExecutable();
        Path workspace = resolveWorkspace(safeRequest);
        Path runDirectory = Files.createTempDirectory(workspace, ".tavall-codex-");
        Path promptPath = runDirectory.resolve("prompt.txt");
        Path lastMessagePath = runDirectory.resolve("last-message.txt");
        Files.writeString(promptPath, buildPrompt(safeRequest), StandardCharsets.UTF_8);

        Process process = null;
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(commandBuilder.build(lastMessagePath));
            processBuilder.directory(workspace.toFile());
            processBuilder.redirectInput(promptPath.toFile());
            replaceEnvironment(processBuilder.environment(), sanitizedEnvironment(System.getenv()));
            Process startedProcess = processBuilder.start();
            process = startedProcess;

            try (ExecutorService outputReaders = Executors.newVirtualThreadPerTaskExecutor()) {
                Future<String> eventsFuture = outputReaders.submit(
                        () -> readBounded(startedProcess.getInputStream(), MAX_CAPTURE_BYTES)
                );
                Future<String> stderrFuture = outputReaders.submit(
                        () -> readBounded(startedProcess.getErrorStream(), MAX_CAPTURE_BYTES)
                );

                final int exitCode;
                try {
                    exitCode = startedProcess.waitFor();
                } catch (InterruptedException exception) {
                    stopProcessTree(startedProcess);
                    eventsFuture.cancel(true);
                    stderrFuture.cancel(true);
                    Thread.currentThread().interrupt();
                    throw exception;
                }

                String message = readBoundedFile(lastMessagePath, MAX_CAPTURE_BYTES);
                String events = awaitCapture(eventsFuture);
                String stderr = awaitCapture(stderrFuture);
                ObjectNode output = objectMapper.createObjectNode();
                output.put("message", message);
                output.put("eventsJsonl", events);
                output.put("stderr", stderr);
                output.put("exitCode", exitCode);
                output.put("workspace", workspace.toString());

                if (exitCode != 0) {
                    String errorMessage = stderr.isBlank()
                            ? "Codex exited with code " + exitCode
                            : "Codex exited with code " + exitCode + ": " + firstLine(stderr);
                    return new AIAgentExecutionResult(
                            AIAgentExecutionStatus.FAILED,
                            output,
                            0,
                            0,
                            errorMessage
                    );
                }
                return new AIAgentExecutionResult(
                        AIAgentExecutionStatus.COMPLETED,
                        output,
                        0,
                        0,
                        ""
                );
            }
        } finally {
            if (process != null && (process.isAlive() || process.toHandle().descendants().anyMatch(ProcessHandle::isAlive))) {
                stopProcessTree(process);
            }
            deleteRecursively(runDirectory);
        }
    }

    private Path resolveWorkspace(AIAgentExecutionRequest request) throws IOException {
        Path resolved = Objects.requireNonNull(workspaceResolver.resolve(request), "workspaceResolver result");
        if (!resolved.isAbsolute()) {
            throw new IllegalArgumentException("Codex workspace lease must be an absolute path");
        }
        Path realPath = resolved.toRealPath();
        if (!Files.isDirectory(realPath)) {
            throw new IllegalArgumentException("Codex workspace lease is not a directory: " + realPath);
        }
        if (!Files.exists(realPath.resolve(".git"))) {
            throw new IllegalArgumentException(
                    "Codex workspace lease must be the root of a trusted Git repository: " + realPath
            );
        }
        return realPath;
    }

    private void validateExecutable() {
        if (!Files.isRegularFile(configuration.executable()) || !Files.isExecutable(configuration.executable())) {
            throw new IllegalArgumentException(
                    "Codex executable is unavailable or not executable: " + configuration.executable()
            );
        }
    }

    private String buildPrompt(AIAgentExecutionRequest request) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a delegated Tavall development-workspace implementation worker.\n")
                .append("Operate only inside the current authorized workspace.\n")
                .append("Do not attempt to reach Tavall production/control infrastructure directly.\n")
                .append("The parent Tavall agent owns remote Function Catalog operations and approvals.\n\n")
                .append("Agent: ").append(request.definition().id()).append('\n')
                .append("Role instructions:\n").append(request.definition().instructions()).append("\n\n")
                .append("Task:\n").append(request.job().task()).append("\n");
        if (!request.job().attributes().isEmpty()) {
            prompt.append("\nJob metadata:\n");
            request.job().attributes().entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> prompt
                            .append(entry.getKey()).append(" = ").append(entry.getValue()).append('\n'));
        }
        return prompt.toString();
    }

    static Map<String, String> sanitizedEnvironment(Map<String, String> source) {
        Objects.requireNonNull(source, "source");
        Map<String, String> result = new LinkedHashMap<>();
        for (String name : INHERITED_ENVIRONMENT_ALLOWLIST) {
            String value = source.get(name);
            if (value != null && !value.isBlank()) {
                result.put(name, value);
            }
        }
        return Map.copyOf(result);
    }

    private static void replaceEnvironment(Map<String, String> target, Map<String, String> sanitized) {
        target.clear();
        target.putAll(sanitized);
    }

    private String awaitCapture(Future<String> capture) throws IOException, InterruptedException {
        try {
            return capture.get();
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof IOException ioException) throw ioException;
            if (cause instanceof RuntimeException runtimeException) throw runtimeException;
            throw new IOException("Unable to capture Codex process output", cause);
        }
    }

    private String readBounded(InputStream input, int maximumBytes) throws IOException {
        byte[] ring = new byte[maximumBytes];
        byte[] buffer = new byte[8192];
        int writeIndex = 0;
        int retained = 0;
        int read;
        while ((read = input.read(buffer)) >= 0) {
            for (int index = 0; index < read; index++) {
                ring[writeIndex] = buffer[index];
                writeIndex = (writeIndex + 1) % ring.length;
                retained = Math.min(retained + 1, ring.length);
            }
        }
        return decodeRing(ring, writeIndex, retained);
    }

    private String readBoundedFile(Path path, int maximumBytes) throws IOException {
        if (!Files.exists(path)) return "";
        long size = Files.size(path);
        long start = Math.max(0L, size - maximumBytes);
        try (var channel = Files.newByteChannel(path)) {
            channel.position(start);
            byte[] bytes = new byte[(int) Math.min(maximumBytes, size)];
            java.nio.ByteBuffer buffer = java.nio.ByteBuffer.wrap(bytes);
            while (buffer.hasRemaining() && channel.read(buffer) >= 0) {
                // Drain only the bounded tail.
            }
            return new String(bytes, 0, buffer.position(), StandardCharsets.UTF_8);
        }
    }

    private String decodeRing(byte[] ring, int writeIndex, int retained) {
        if (retained == 0) return "";
        byte[] ordered = new byte[retained];
        int start = retained == ring.length ? writeIndex : 0;
        for (int index = 0; index < retained; index++) {
            ordered[index] = ring[(start + index) % ring.length];
        }
        return new String(ordered, StandardCharsets.UTF_8);
    }

    private String firstLine(String value) {
        int newline = value.indexOf('\n');
        return newline < 0 ? value : value.substring(0, newline);
    }

    private void stopProcessTree(Process process) {
        ProcessHandle root = process.toHandle();
        LinkedHashSet<ProcessHandle> observed = new LinkedHashSet<>();
        observeTree(root, observed);
        destroyObserved(root, observed, false);
        waitForExit(observed, 1, TimeUnit.SECONDS);

        for (int attempt = 0; attempt < 3 && observed.stream().anyMatch(ProcessHandle::isAlive); attempt++) {
            for (ProcessHandle handle : List.copyOf(observed)) {
                if (handle.isAlive()) {
                    observeTree(handle, observed);
                }
            }
            destroyObserved(root, observed, true);
            waitForExit(observed, 1, TimeUnit.SECONDS);
        }
    }

    private void observeTree(ProcessHandle handle, Set<ProcessHandle> observed) {
        observed.add(handle);
        handle.descendants().forEach(observed::add);
    }

    private void destroyObserved(ProcessHandle root, Set<ProcessHandle> observed, boolean forcibly) {
        observed.stream()
                .filter(ProcessHandle::isAlive)
                .filter(handle -> handle.pid() != root.pid())
                .sorted(Comparator.comparingLong(ProcessHandle::pid).reversed())
                .forEach(handle -> {
                    if (forcibly) handle.destroyForcibly(); else handle.destroy();
                });
        if (root.isAlive()) {
            if (forcibly) root.destroyForcibly(); else root.destroy();
        }
    }

    private void waitForExit(Set<ProcessHandle> handles, long timeout, TimeUnit unit) {
        long deadline = System.nanoTime() + unit.toNanos(timeout);
        while (handles.stream().anyMatch(ProcessHandle::isAlive) && System.nanoTime() < deadline) {
            try {
                Thread.sleep(25L);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void deleteRecursively(Path root) {
        if (root == null || !Files.exists(root)) return;
        try (var paths = Files.walk(root)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                    // Authoritative workspace cleanup remains owned by the lease owner.
                }
            });
        } catch (IOException ignored) {
            // Never touch paths outside the provider-created directory.
        }
    }
}

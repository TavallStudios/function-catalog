package org.tavall.ai.agent.codex;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.tavall.ai.agent.AIAgentExecutionRequest;
import org.tavall.ai.agent.AIAgentExecutionResult;
import org.tavall.ai.agent.AIAgentExecutionStatus;
import org.tavall.ai.agent.AIAgentProvider;
import org.tavall.ai.agent.codex.CodexProcessIsolationSupervisor.CodexSupervisedProcessRequest;
import org.tavall.ai.agent.codex.CodexProcessIsolationSupervisor.CodexSupervisedProcessResult;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Development-workspace Codex CLI provider.
 *
 * <p>This provider deliberately does not expose the Tavall Function Catalog to Codex directly.
 * It is an implementation worker for an already-authorized workspace lease. Tavall remote
 * operations remain owned by the parent agent and CONTROL until a separately reviewed typed
 * bridge is introduced.</p>
 *
 * <p>Codex is never spawned directly by this class. A host-owned
 * {@link CodexProcessIsolationSupervisor} is mandatory so the Tavall Cloud development worker,
 * rather than this provider, owns process identity, private process visibility, process-group or
 * cgroup lifetime, cancellation, and bounded output capture.</p>
 */
public final class CodexAgentProvider implements AIAgentProvider {
    public static final String PROVIDER_ID = "codex";
    private static final int MAX_CAPTURE_BYTES = 256 * 1024;
    private static final Set<String> INHERITED_ENVIRONMENT_ALLOWLIST = Set.of(
            "PATH",
            "PATHEXT",
            "SystemRoot",
            "SYSTEMROOT",
            "WINDIR",
            "COMSPEC",
            "HOME",
            "USERPROFILE",
            "TMP",
            "TEMP",
            "TMPDIR",
            "LANG",
            "LC_ALL",
            "TERM",
            "CODEX_HOME",
            "OPENAI_API_KEY",
            "OPENAI_BASE_URL",
            "OPENAI_ORG_ID",
            "OPENAI_PROJECT_ID",
            "SSL_CERT_FILE",
            "SSL_CERT_DIR"
    );

    private final CodexAgentProviderConfiguration configuration;
    private final CodexWorkspaceResolver workspaceResolver;
    private final CodexProcessIsolationSupervisor processSupervisor;
    private final ObjectMapper objectMapper;
    private final CodexCommandBuilder commandBuilder;

    public CodexAgentProvider(
            CodexAgentProviderConfiguration configuration,
            CodexWorkspaceResolver workspaceResolver,
            CodexProcessIsolationSupervisor processSupervisor,
            ObjectMapper objectMapper
    ) {
        this.configuration = Objects.requireNonNull(configuration, "configuration");
        this.workspaceResolver = Objects.requireNonNull(workspaceResolver, "workspaceResolver");
        this.processSupervisor = Objects.requireNonNull(processSupervisor, "processSupervisor");
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

        try {
            CodexSupervisedProcessResult processResult = processSupervisor.execute(
                    new CodexSupervisedProcessRequest(
                            commandBuilder.build(lastMessagePath),
                            workspace,
                            promptPath,
                            sanitizedEnvironment(System.getenv()),
                            MAX_CAPTURE_BYTES
                    )
            );
            requireBoundedCapture("stdout", processResult.stdout());
            requireBoundedCapture("stderr", processResult.stderr());

            String message = readBoundedFile(lastMessagePath, MAX_CAPTURE_BYTES);
            ObjectNode output = objectMapper.createObjectNode();
            output.put("message", message);
            output.put("eventsJsonl", processResult.stdout());
            output.put("stderr", processResult.stderr());
            output.put("exitCode", processResult.exitCode());
            output.put("workspace", workspace.toString());

            if (processResult.exitCode() != 0) {
                String errorMessage = processResult.stderr().isBlank()
                        ? "Codex exited with code " + processResult.exitCode()
                        : "Codex exited with code " + processResult.exitCode()
                        + ": " + firstLine(processResult.stderr());
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
        } finally {
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
        validateGitRepositoryRoot(realPath);
        return realPath;
    }

    private void validateGitRepositoryRoot(Path workspace) throws IOException {
        Process gitProcess = new ProcessBuilder(
                "git",
                "-C",
                workspace.toString(),
                "rev-parse",
                "--show-toplevel"
        ).redirectErrorStream(true).start();
        try {
            String output = new String(gitProcess.getInputStream().readAllBytes(), StandardCharsets.UTF_8).strip();
            if (!gitProcess.waitFor(5, TimeUnit.SECONDS)) {
                gitProcess.destroyForcibly();
                throw new IllegalArgumentException("Unable to validate the Codex workspace Git root: " + workspace);
            }
            if (gitProcess.exitValue() != 0 || output.isBlank()) {
                throw new IllegalArgumentException(
                        "Codex workspace lease must be the root of a trusted Git repository: " + workspace
                );
            }
            Path gitRoot = Path.of(output).toRealPath();
            if (!gitRoot.equals(workspace)) {
                throw new IllegalArgumentException(
                        "Codex workspace lease must be the root of a trusted Git repository: " + workspace
                );
            }
        } catch (InterruptedException exception) {
            gitProcess.destroyForcibly();
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while validating the Codex workspace Git root", exception);
        }
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
                            .append(entry.getKey())
                            .append(" = ")
                            .append(entry.getValue())
                            .append('\n'));
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

    private void requireBoundedCapture(String streamName, String value) {
        if (value.getBytes(StandardCharsets.UTF_8).length > MAX_CAPTURE_BYTES) {
            throw new IllegalStateException(
                    "Codex process supervisor exceeded the " + streamName + " capture budget"
            );
        }
    }

    private String readBoundedFile(Path path, int maximumBytes) throws IOException {
        if (!Files.exists(path)) {
            return "";
        }
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

    private String firstLine(String value) {
        int newline = value.indexOf('\n');
        return newline < 0 ? value : value.substring(0, newline);
    }

    private void deleteRecursively(Path root) {
        if (root == null || !Files.exists(root)) {
            return;
        }
        try (var paths = Files.walk(root)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                    // Best-effort cleanup; the workspace lease owner performs authoritative cleanup.
                }
            });
        } catch (IOException ignored) {
            // Best-effort cleanup; no workspace path outside the provider-created directory is touched.
        }
    }
}

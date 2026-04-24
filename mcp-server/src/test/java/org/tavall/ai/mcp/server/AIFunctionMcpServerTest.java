package org.tavall.ai.mcp.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AIFunctionMcpServerTest {
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void shouldListAndCallRegistrarBackedFunctionsOverStdio() throws Exception {
        Path tempDirectory = Files.createTempDirectory("function-catalog-mcp");
        Path stateFile = tempDirectory.resolve("state.json");
        Path snapshotFile = tempDirectory.resolve("snapshot.json");
        String nonce = "nonce-" + UUID.randomUUID();

        try (McpSyncClient client = startClient(
                registrarArgs(stateFile, snapshotFile),
                Map.of("FUNCTION_CATALOG_NONCE", nonce))) {
            client.initialize();
            assertTrue(client.listTools().tools().stream().anyMatch(tool -> "codex_nonce".equals(tool.name())));

            McpSchema.CallToolResult result = client.callTool(new McpSchema.CallToolRequest("codex_nonce", Map.of()));
            assertFalse(Boolean.TRUE.equals(result.isError()));
            JsonNode payload = payload(result);
            assertEquals(nonce, payload.path("nonce").asText());
            assertTrue(Files.exists(snapshotFile));
        }
    }

    @Test
    void shouldSupportFallbackPackageScanningOverStdio() throws Exception {
        Path tempDirectory = Files.createTempDirectory("function-catalog-scan-mcp");
        Path stateFile = tempDirectory.resolve("state.json");
        Path snapshotFile = tempDirectory.resolve("snapshot.json");

        try (McpSyncClient client = startClient(
                List.of(
                        "--scan=org.tavall.ai.mcp.server.scanfixtures",
                        "--state-file=" + stateFile,
                        "--snapshot-file=" + snapshotFile
                ),
                Map.of())) {
            client.initialize();
            assertTrue(client.listTools().tools().stream().anyMatch(tool -> "scan_echo".equals(tool.name())));

            McpSchema.CallToolResult result = client.callTool(new McpSchema.CallToolRequest("scan_echo", Map.of("value", "abc")));
            assertFalse(Boolean.TRUE.equals(result.isError()));
            assertEquals("scan:abc", payload(result).asText());
        }
    }

    @Test
    void shouldLetCodexCallNonceFunctionThroughEphemeralMcpConfig() throws Exception {
        assertCodexAvailableAndLoggedIn();

        Path tempDirectory = Files.createTempDirectory("function-catalog-codex");
        Path stateFile = tempDirectory.resolve("state.json");
        Path snapshotFile = tempDirectory.resolve("snapshot.json");
        String nonce = "nonce-" + UUID.randomUUID();
        Path outputFile = tempDirectory.resolve("codex-output.json");
        Path outputSchemaFile = tempDirectory.resolve("codex-output-schema.json");
        String serverName = "functioncatalog-test-" + UUID.randomUUID().toString().replace("-", "");

        registerCodexMcpServer(serverName, stateFile, snapshotFile, nonce);
        try {
            ExecResult result = runProcess(List.of(
                    codexExecutable(),
                    "exec",
                    "--color", "never",
                    "--ephemeral",
                    "--dangerously-bypass-approvals-and-sandbox",
                    "--output-schema", writeNonceOutputSchema(outputSchemaFile).toString(),
                    "-o", outputFile.toString(),
                    "Use the MCP tool named codex_nonce exactly once with no arguments. Return only minified JSON with a single key nonce whose value is the tool result nonce."
            ), repoRoot(), Duration.ofMinutes(3));

            assertEquals(0, result.exitCode(), result.stderr());
            JsonNode response = objectMapper.readTree(Files.readString(outputFile));
            assertEquals(nonce, response.path("nonce").asText(), result.stdout() + System.lineSeparator() + result.stderr());
        } finally {
            removeCodexMcpServer(serverName);
        }
    }

    @Test
    void shouldReturnDisabledErrorWhenCodexCallsDisabledFunction() throws Exception {
        assertCodexAvailableAndLoggedIn();

        Path tempDirectory = Files.createTempDirectory("function-catalog-codex-disabled");
        Path stateFile = tempDirectory.resolve("state.json");
        Path snapshotFile = tempDirectory.resolve("snapshot.json");
        String nonce = "nonce-" + UUID.randomUUID();
        Files.writeString(stateFile, disabledStateJson());
        Files.setLastModifiedTime(stateFile, java.nio.file.attribute.FileTime.from(Instant.now().plusSeconds(1)));
        Path outputFile = tempDirectory.resolve("codex-output-disabled.json");
        Path outputSchemaFile = tempDirectory.resolve("codex-output-disabled-schema.json");
        String serverName = "functioncatalog-test-" + UUID.randomUUID().toString().replace("-", "");

        registerCodexMcpServer(serverName, stateFile, snapshotFile, nonce);
        try {
            ExecResult result = runProcess(List.of(
                    codexExecutable(),
                    "exec",
                    "--color", "never",
                    "--ephemeral",
                    "--dangerously-bypass-approvals-and-sandbox",
                    "--output-schema", writeDisabledOutputSchema(outputSchemaFile).toString(),
                    "-o", outputFile.toString(),
                    "Use the MCP tool named codex_nonce exactly once with no arguments. If the tool call fails, return only minified JSON with keys success and errorCode."
            ), repoRoot(), Duration.ofMinutes(3));

            assertEquals(0, result.exitCode(), result.stderr());
            JsonNode response = objectMapper.readTree(Files.readString(outputFile));
            assertFalse(response.path("success").asBoolean(true));
            assertEquals("disabled", response.path("errorCode").asText(), result.stdout() + System.lineSeparator() + result.stderr());
        } finally {
            removeCodexMcpServer(serverName);
        }
    }

    private McpSyncClient startClient(List<String> launcherArgs, Map<String, String> env) {
        StdioClientTransport transport = new StdioClientTransport(
                ServerParameters.builder(javaExecutable())
                        .args(serverProcessArgs(launcherArgs))
                        .env(env)
                        .build(),
                new JacksonMcpJsonMapper(objectMapper)
        );
        return McpClient.sync(transport)
                .requestTimeout(Duration.ofSeconds(20))
                .initializationTimeout(Duration.ofSeconds(20))
                .clientInfo(new McpSchema.Implementation("FunctionCatalog Tests", "1.0.0"))
                .build();
    }

    private List<String> registrarArgs(Path stateFile, Path snapshotFile) {
        return List.of(
                "--registrar-class=org.tavall.ai.mcp.server.fixtures.NonceRegistrar",
                "--state-file=" + stateFile,
                "--snapshot-file=" + snapshotFile
        );
    }

    private List<String> serverProcessArgs(List<String> launcherArgs) {
        List<String> args = new ArrayList<>();
        args.add("-cp");
        args.add(System.getProperty("java.class.path"));
        args.add("org.tavall.ai.mcp.server.AIFunctionMcpServerLauncher");
        args.addAll(launcherArgs);
        return args;
    }

    private JsonNode payload(McpSchema.CallToolResult result) throws Exception {
        Object structured = result.structuredContent();
        if (structured != null) {
            return objectMapper.valueToTree(structured);
        }
        if (result.content() != null && !result.content().isEmpty() && result.content().getFirst() instanceof McpSchema.TextContent textContent) {
            return objectMapper.readTree(textContent.text());
        }
        throw new IllegalStateException("MCP tool result did not include any payload.");
    }

    private void assertCodexAvailableAndLoggedIn() throws Exception {
        ExecResult status = runProcess(List.of(codexExecutable(), "login", "status"), repoRoot(), Duration.ofSeconds(30));
        assertEquals(0, status.exitCode(), status.stderr());
        String combined = status.stdout() + System.lineSeparator() + status.stderr();
        assertTrue(combined.contains("Logged in"), combined);
    }

    private void registerCodexMcpServer(String serverName, Path stateFile, Path snapshotFile, String nonce) throws Exception {
        ExecResult addResult = runProcess(List.of(
                codexExecutable(),
                "mcp",
                "add",
                serverName,
                "--env", "FUNCTION_CATALOG_NONCE=" + nonce,
                "--",
                javaExecutable(),
                "-cp", System.getProperty("java.class.path"),
                "org.tavall.ai.mcp.server.AIFunctionMcpServerLauncher",
                "--registrar-class=org.tavall.ai.mcp.server.fixtures.NonceRegistrar",
                "--state-file=" + stateFile,
                "--snapshot-file=" + snapshotFile
        ), repoRoot(), Duration.ofSeconds(30));
        assertEquals(0, addResult.exitCode(), addResult.stderr());
    }

    private void removeCodexMcpServer(String serverName) throws Exception {
        ExecResult removeResult = runProcess(
                List.of(codexExecutable(), "mcp", "remove", serverName),
                repoRoot(),
                Duration.ofSeconds(30)
        );
        if (removeResult.exitCode() != 0) {
            throw new IllegalStateException("Failed to remove Codex MCP server '" + serverName + "': "
                    + removeResult.stdout() + System.lineSeparator() + removeResult.stderr());
        }
    }

    private String disabledStateJson() throws Exception {
        Map<String, Object> root = new HashMap<>();
        root.put("updatedAt", Instant.now().toString());
        root.put("functions", Map.of(
                "codex_nonce", Map.of(
                        "enabled", false,
                        "updatedAt", Instant.now().toString()
                )
        ));
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
    }

    private Path writeNonceOutputSchema(Path schemaPath) throws Exception {
        return writeOutputSchema(schemaPath, Map.of(
                "type", "object",
                "additionalProperties", false,
                "required", List.of("nonce"),
                "properties", Map.of(
                        "nonce", Map.of("type", "string")
                )
        ));
    }

    private Path writeDisabledOutputSchema(Path schemaPath) throws Exception {
        return writeOutputSchema(schemaPath, Map.of(
                "type", "object",
                "additionalProperties", false,
                "required", List.of("success", "errorCode"),
                "properties", Map.of(
                        "success", Map.of("type", "boolean"),
                        "errorCode", Map.of("type", "string")
                )
        ));
    }

    private Path writeOutputSchema(Path schemaPath, Map<String, Object> schema) throws Exception {
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(schemaPath.toFile(), schema);
        return schemaPath;
    }

    private String javaExecutable() {
        return Path.of(System.getProperty("java.home"), "bin", isWindows() ? "java.exe" : "java").toString();
    }

    private String codexExecutable() {
        List<Path> candidates = List.of(
                Path.of(System.getenv().getOrDefault("APPDATA", ""), "npm", "codex.cmd"),
                Path.of(System.getProperty("user.home"), ".local", "bin", "codex.cmd"),
                Path.of("C:\\Program Files\\WindowsApps\\OpenAI.Codex_26.325.3894.0_x64__2p2nqsd0c76g0\\app\\resources\\codex.exe")
        );
        for (Path candidate : candidates) {
            if (Files.isRegularFile(candidate)) {
                return candidate.toString();
            }
        }
        return "codex.cmd";
    }

    private boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    private Path repoRoot() {
        Path current = Path.of("").toAbsolutePath().normalize();
        return current.getFileName() != null && "mcp-server".equals(current.getFileName().toString())
                ? current.getParent()
                : current;
    }

    private ExecResult runProcess(List<String> command, Path workingDirectory, Duration timeout) throws Exception {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(workingDirectory.toFile());
        Process process = builder.start();
        boolean finished = process.waitFor(timeout.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new IllegalStateException("Process timed out: " + String.join(" ", command));
        }
        return new ExecResult(process.exitValue(), readFully(process.getInputStream()), readFully(process.getErrorStream()));
    }

    private String readFully(InputStream stream) throws IOException {
        try (InputStream safeStream = stream; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            safeStream.transferTo(output);
            return output.toString(StandardCharsets.UTF_8);
        }
    }

    private record ExecResult(int exitCode, String stdout, String stderr) {
    }
}

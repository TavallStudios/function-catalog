package org.tavall.ai.agent.strands;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.jackson2.JacksonMcpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/** Lifecycle owner for the Java -> standalone Strands MCP connection. */
public final class StrandsBridgeMcpClient implements AutoCloseable {
    public static final String CREATE_AGENT_TOOL = "strands_agent_create";
    public static final String INVOKE_ONCE_TOOL = "strands_agent_invoke_once";
    public static final String CLOSE_AGENT_TOOL = "strands_agent_close";

    private static final Logger LOGGER = LoggerFactory.getLogger(StrandsBridgeMcpClient.class);

    private final StrandsAgentProviderConfiguration configuration;
    private final ObjectMapper objectMapper;
    private final AtomicBoolean closed = new AtomicBoolean();
    private volatile McpSyncClient client;

    public StrandsBridgeMcpClient(StrandsAgentProviderConfiguration configuration) {
        this(configuration, new ObjectMapper());
    }

    StrandsBridgeMcpClient(
            StrandsAgentProviderConfiguration configuration,
            ObjectMapper objectMapper
    ) {
        this.configuration = Objects.requireNonNull(configuration, "configuration");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    /** Creates a sessionful Strands runtime through the standalone bridge. */
    public void createAgent(Map<String, Object> runtimeConfig) {
        Objects.requireNonNull(runtimeConfig, "runtimeConfig");
        callForSuccess(CREATE_AGENT_TOOL, Map.of("config", runtimeConfig));
    }

    /** Closes a sessionful Strands runtime through the standalone bridge. */
    public void closeAgent(String agentId) {
        String safeAgentId = requireText(agentId, "agentId");
        callForSuccess(CLOSE_AGENT_TOOL, Map.of("agentId", safeAgentId));
    }

    public String invokeOnce(Map<String, Object> runtimeConfig, String input) {
        Objects.requireNonNull(runtimeConfig, "runtimeConfig");
        String safeInput = Objects.requireNonNull(input, "input");
        McpSchema.CallToolResult result = client().callTool(new McpSchema.CallToolRequest(
                INVOKE_ONCE_TOOL,
                Map.of(
                        "config", runtimeConfig,
                        "input", safeInput
                ),
                null
        ));

        String text = firstText(result.content());
        if (Boolean.TRUE.equals(result.isError())) {
            throw new IllegalStateException(text.isBlank()
                    ? "Standalone Strands MCP runtime reported an error."
                    : text);
        }
        if (text.isBlank()) {
            throw new IllegalStateException("Standalone Strands MCP runtime returned no text result.");
        }
        return text;
    }

    private void callForSuccess(String toolName, Map<String, Object> arguments) {
        McpSchema.CallToolResult result = client().callTool(new McpSchema.CallToolRequest(
                toolName,
                arguments,
                null
        ));
        if (Boolean.TRUE.equals(result.isError())) {
            String text = firstText(result.content());
            throw new IllegalStateException(text.isBlank()
                    ? "Standalone Strands MCP runtime tool failed: " + toolName
                    : text);
        }
    }

    private McpSyncClient client() {
        McpSyncClient existing = client;
        if (existing != null) {
            return existing;
        }

        synchronized (this) {
            if (closed.get()) {
                throw new IllegalStateException("Strands bridge MCP client is closed.");
            }
            if (client != null) {
                return client;
            }

            ServerParameters serverParameters = ServerParameters.builder(configuration.command())
                    .args(configuration.arguments())
                    .env(configuration.environment())
                    .build();
            McpJsonMapper jsonMapper = new JacksonMcpJsonMapper(objectMapper);
            StdioClientTransport transport = new IsolatedStdioClientTransport(
                    serverParameters,
                    jsonMapper,
                    configuration.environment()
            );
            transport.setStdErrorHandler(message -> LOGGER.info("[strands-bridge] {}", message));

            McpSyncClient created = McpClient.sync(transport)
                    .initializationTimeout(configuration.initializationTimeout())
                    .requestTimeout(configuration.requestTimeout())
                    .build();
            try {
                created.initialize();
                boolean hasInvokeOnce = created.listTools().tools().stream()
                        .anyMatch(tool -> INVOKE_ONCE_TOOL.equals(tool.name()));
                if (!hasInvokeOnce) {
                    throw new IllegalStateException(
                            "Standalone Strands MCP runtime does not expose " + INVOKE_ONCE_TOOL
                    );
                }
                client = created;
                return created;
            } catch (RuntimeException exception) {
                created.closeGracefully();
                throw exception;
            }
        }
    }

    private static String firstText(List<McpSchema.Content> content) {
        if (content == null) {
            return "";
        }
        for (McpSchema.Content item : content) {
            if (item instanceof McpSchema.TextContent textContent) {
                return textContent.text() == null ? "" : textContent.text();
            }
        }
        return "";
    }

    private static String requireText(String value, String fieldName) {
        if (value != null && !value.isBlank()) {
            return value.trim();
        }
        throw new IllegalArgumentException(fieldName + " must not be blank");
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        McpSyncClient existing = client;
        client = null;
        if (existing != null) {
            existing.closeGracefully();
        }
    }

    /** Prevents product secrets in the Java process environment from leaking into Strands. */
    private static final class IsolatedStdioClientTransport extends StdioClientTransport {
        private final Map<String, String> isolatedEnvironment;

        private IsolatedStdioClientTransport(
                ServerParameters serverParameters,
                McpJsonMapper jsonMapper,
                Map<String, String> isolatedEnvironment
        ) {
            super(serverParameters, jsonMapper);
            this.isolatedEnvironment = Map.copyOf(isolatedEnvironment);
        }

        @Override
        protected ProcessBuilder getProcessBuilder() {
            ProcessBuilder processBuilder = super.getProcessBuilder();
            processBuilder.environment().clear();
            processBuilder.environment().putAll(isolatedEnvironment);
            return processBuilder;
        }
    }
}

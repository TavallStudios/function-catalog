package org.tavall.ai.agent.strands;

import com.fasterxml.jackson.databind.JsonNode;
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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/** Lifecycle owner for the Java -> standalone Strands MCP connection. */
public final class StrandsBridgeMcpClient implements AutoCloseable {
    public static final String CREATE_AGENT_TOOL = "strands_agent_create";
    public static final String INVOKE_AGENT_TOOL = "strands_agent_invoke";
    public static final String INVOKE_OBSERVED_AGENT_TOOL = "strands_agent_invoke_observed";
    public static final String CANCEL_AGENT_TOOL = "strands_agent_cancel";
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

    /** Creates an independent sessionful Strands runtime through the standalone bridge. */
    public void createAgent(Map<String, Object> runtimeConfig) {
        createAgent(runtimeConfig, List.of());
    }

    /**
     * Creates a sessionful Strands runtime and composes already-live sessions as native
     * Strands agent-as-tool capabilities inside the standalone bridge process.
     */
    public void createAgent(
            Map<String, Object> runtimeConfig,
            List<StrandsAgentToolReference> agentTools
    ) {
        Objects.requireNonNull(runtimeConfig, "runtimeConfig");
        List<StrandsAgentToolReference> safeAgentTools = List.copyOf(
                Objects.requireNonNull(agentTools, "agentTools")
        );
        Map<String, Object> arguments = new LinkedHashMap<>();
        arguments.put("config", runtimeConfig);
        if (!safeAgentTools.isEmpty()) {
            arguments.put(
                    "agentTools",
                    safeAgentTools.stream().map(StrandsAgentToolReference::toMcpValue).toList()
            );
        }
        callForSuccess(CREATE_AGENT_TOOL, Map.copyOf(arguments));
    }

    /** Invokes one already-live sessionful Strands runtime. */
    public String invokeAgent(String agentId, String input) {
        String safeAgentId = requireText(agentId, "agentId");
        String safeInput = Objects.requireNonNull(input, "input");
        return callForText(
                INVOKE_AGENT_TOOL,
                Map.of("agentId", safeAgentId, "input", safeInput)
        );
    }

    /**
     * Invokes a live Strands runtime and returns the normalized native tool lifecycle
     * evidence emitted during that invocation. Product-specific trust policy remains
     * outside this shared transport client.
     */
    public StrandsObservedInvocationResult invokeAgentObserved(
            String agentId,
            String input,
            StrandsInvocationLimits limits
    ) {
        String safeAgentId = requireText(agentId, "agentId");
        String safeInput = Objects.requireNonNull(input, "input");
        Objects.requireNonNull(limits, "limits");

        McpSchema.CallToolResult result = callTool(
                INVOKE_OBSERVED_AGENT_TOOL,
                Map.of(
                        "agentId", safeAgentId,
                        "input", safeInput,
                        "limits", limits.toMcpValue()
                )
        );
        assertSuccess(INVOKE_OBSERVED_AGENT_TOOL, result);

        JsonNode structured = objectMapper.valueToTree(result.structuredContent());
        if (structured == null || !structured.isObject()) {
            throw new IllegalStateException("Standalone Strands MCP runtime returned no structured observed invocation result.");
        }

        String returnedAgentId = requiredText(structured, "agentId");
        String text = requiredText(structured, "text");
        String stopReason = optionalText(structured, "stopReason");
        List<StrandsObservedToolEvent> toolEvents = new ArrayList<>();
        JsonNode events = structured.get("toolEvents");
        if (events == null || !events.isArray()) {
            throw new IllegalStateException("Standalone Strands MCP runtime returned no toolEvents array.");
        }
        for (JsonNode event : events) {
            if (!event.isObject()) {
                throw new IllegalStateException("Standalone Strands MCP runtime returned an invalid tool event.");
            }
            Map<String, Object> eventInput = Map.of();
            JsonNode inputNode = event.get("input");
            if (inputNode != null && inputNode.isObject()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> converted = objectMapper.convertValue(inputNode, Map.class);
                eventInput = Map.copyOf(converted);
            }
            toolEvents.add(new StrandsObservedToolEvent(
                    requiredText(event, "agentId"),
                    requiredText(event, "toolName"),
                    eventInput,
                    optionalText(event, "status"),
                    optionalText(event, "error")
            ));
        }
        return new StrandsObservedInvocationResult(returnedAgentId, text, stopReason, toolEvents);
    }

    /** Cooperatively cancels the active invocation of one already-live Strands runtime. */
    public void cancelAgent(String agentId) {
        String safeAgentId = requireText(agentId, "agentId");
        callForSuccess(CANCEL_AGENT_TOOL, Map.of("agentId", safeAgentId));
    }

    /** Closes a sessionful Strands runtime through the standalone bridge. */
    public void closeAgent(String agentId) {
        String safeAgentId = requireText(agentId, "agentId");
        callForSuccess(CLOSE_AGENT_TOOL, Map.of("agentId", safeAgentId));
    }

    public String invokeOnce(Map<String, Object> runtimeConfig, String input) {
        Objects.requireNonNull(runtimeConfig, "runtimeConfig");
        String safeInput = Objects.requireNonNull(input, "input");
        return callForText(
                INVOKE_ONCE_TOOL,
                Map.of(
                        "config", runtimeConfig,
                        "input", safeInput
                )
        );
    }

    private String callForText(String toolName, Map<String, Object> arguments) {
        McpSchema.CallToolResult result = callTool(toolName, arguments);
        assertSuccess(toolName, result);
        String text = firstText(result.content());
        if (text.isBlank()) {
            throw new IllegalStateException("Standalone Strands MCP runtime returned no text result for " + toolName + ".");
        }
        return text;
    }

    private void callForSuccess(String toolName, Map<String, Object> arguments) {
        assertSuccess(toolName, callTool(toolName, arguments));
    }

    private McpSchema.CallToolResult callTool(String toolName, Map<String, Object> arguments) {
        return client().callTool(new McpSchema.CallToolRequest(
                toolName,
                arguments,
                null
        ));
    }

    private static void assertSuccess(String toolName, McpSchema.CallToolResult result) {
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
                List<String> requiredTools = List.of(
                        CREATE_AGENT_TOOL,
                        INVOKE_AGENT_TOOL,
                        INVOKE_OBSERVED_AGENT_TOOL,
                        CANCEL_AGENT_TOOL,
                        INVOKE_ONCE_TOOL,
                        CLOSE_AGENT_TOOL
                );
                List<String> availableTools = created.listTools().tools().stream()
                        .map(McpSchema.Tool::name)
                        .toList();
                for (String requiredTool : requiredTools) {
                    if (!availableTools.contains(requiredTool)) {
                        throw new IllegalStateException(
                                "Standalone Strands MCP runtime does not expose " + requiredTool
                        );
                    }
                }
                client = created;
                return created;
            } catch (RuntimeException exception) {
                created.closeGracefully();
                throw exception;
            }
        }
    }

    private static String requiredText(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);
        if (value != null && value.isTextual() && !value.textValue().isBlank()) {
            return value.textValue();
        }
        throw new IllegalStateException("Standalone Strands MCP runtime returned invalid " + fieldName + ".");
    }

    private static String optionalText(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);
        return value != null && value.isTextual() ? value.textValue() : null;
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

package org.tavall.ai.agent.strands;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.tavall.ai.agent.AIAgentDefinition;
import org.tavall.ai.agent.AIAgentExecutionRequest;
import org.tavall.ai.agent.AIAgentExecutionResult;
import org.tavall.ai.agent.AIAgentExecutionStatus;
import org.tavall.ai.agent.AIAgentJob;
import org.tavall.ai.agent.AIAgentProvider;
import org.tavall.ai.mcp.server.AIFunctionMcpHttpServer;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Tavall agent provider that delegates reasoning to the standalone Strands MCP runtime.
 *
 * <p>The provider never serializes or recreates Java tool behavior. For each execution it
 * exposes the already-authorized {@link org.tavall.ai.core.catalog.AIFunctionCatalogView}
 * through a loopback-only ephemeral MCP endpoint and gives Strands only that endpoint.</p>
 */
public final class StrandsAgentProvider implements AIAgentProvider, AutoCloseable {
    public static final String PROVIDER_ID = "strands";

    private final StrandsAgentProviderConfiguration configuration;
    private final StrandsBridgeMcpClient bridgeClient;

    public StrandsAgentProvider(StrandsAgentProviderConfiguration configuration) {
        this(configuration, new StrandsBridgeMcpClient(configuration));
    }

    StrandsAgentProvider(
            StrandsAgentProviderConfiguration configuration,
            StrandsBridgeMcpClient bridgeClient
    ) {
        this.configuration = Objects.requireNonNull(configuration, "configuration");
        this.bridgeClient = Objects.requireNonNull(bridgeClient, "bridgeClient");
    }

    @Override
    public String providerId() {
        return PROVIDER_ID;
    }

    @Override
    public AIAgentExecutionResult execute(AIAgentExecutionRequest request) {
        AIAgentExecutionRequest safeRequest = Objects.requireNonNull(request, "request");

        try (AIFunctionMcpHttpServer functionServer = AIFunctionMcpHttpServer.start(safeRequest.functionView())) {
            Map<String, Object> runtimeConfig = runtimeConfig(
                    safeRequest.definition(),
                    safeRequest.job(),
                    functionServer.endpointUri()
            );
            String resultText = bridgeClient.invokeOnce(runtimeConfig, safeRequest.job().task());

            ObjectNode output = JsonNodeFactory.instance.objectNode();
            output.put("text", resultText);
            output.put("agentId", safeRequest.definition().id());
            output.put("jobId", safeRequest.job().jobId());
            return new AIAgentExecutionResult(
                    AIAgentExecutionStatus.COMPLETED,
                    output,
                    0,
                    0,
                    null
            );
        }
    }

    Map<String, Object> runtimeConfig(
            AIAgentDefinition definition,
            AIAgentJob job,
            URI functionMcpEndpoint
    ) {
        AIAgentDefinition safeDefinition = Objects.requireNonNull(definition, "definition");
        AIAgentJob safeJob = Objects.requireNonNull(job, "job");
        URI safeFunctionMcpEndpoint = Objects.requireNonNull(functionMcpEndpoint, "functionMcpEndpoint");

        Map<String, Object> agent = new LinkedHashMap<>();
        agent.put("id", safeDefinition.id());
        agent.put("name", safeDefinition.id());
        agent.put("systemPrompt", safeDefinition.instructions());
        agent.put("printer", false);
        if (!configuration.modelId().isBlank()) {
            agent.put("model", configuration.modelId());
        }
        agent.put("traceAttributes", Map.of(
                "tavall.agent.id", safeDefinition.id(),
                "tavall.job.id", safeJob.jobId(),
                "tavall.provider", PROVIDER_ID
        ));

        Map<String, Object> tavallFunctions = new LinkedHashMap<>();
        tavallFunctions.put("url", safeFunctionMcpEndpoint.toString());
        tavallFunctions.put("transport", "streamable-http");
        tavallFunctions.put("prefix", "tavall");
        tavallFunctions.put("continueOnError", false);

        Map<String, Object> runtime = new LinkedHashMap<>();
        runtime.put("agent", Map.copyOf(agent));
        runtime.put("mcpServers", Map.of("tavall", Map.copyOf(tavallFunctions)));
        return Map.copyOf(runtime);
    }

    @Override
    public void close() {
        bridgeClient.close();
    }
}

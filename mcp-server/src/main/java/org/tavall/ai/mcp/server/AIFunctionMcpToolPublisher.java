package org.tavall.ai.mcp.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema;
import org.tavall.ai.core.catalog.AIFunctionCatalog;
import org.tavall.ai.core.catalog.AIFunctionDefinition;
import org.tavall.ai.core.invocation.AIFunctionInvocationResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Publishes canonical Function Catalog definitions as MCP tool specifications.
 *
 * <p>The catalog remains the source of truth for function names, descriptions,
 * schemas, invocation policy, and audit behavior. Consumers may apply a
 * publication filter to expose a narrower capability view without duplicating
 * tool schemas or invocation adapters.</p>
 */
public final class AIFunctionMcpToolPublisher {
    private final ObjectMapper objectMapper;

    public AIFunctionMcpToolPublisher(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    public List<SyncToolSpecification> toolSpecifications(AIFunctionCatalog catalog) {
        return toolSpecifications(catalog, ignored -> true);
    }

    public List<SyncToolSpecification> toolSpecifications(
            AIFunctionCatalog catalog,
            Predicate<AIFunctionDefinition> publicationFilter
    ) {
        AIFunctionCatalog safeCatalog = Objects.requireNonNull(catalog, "catalog");
        Predicate<AIFunctionDefinition> safePublicationFilter =
                Objects.requireNonNull(publicationFilter, "publicationFilter");

        List<SyncToolSpecification> specifications = new ArrayList<>();
        for (AIFunctionDefinition definition : safeCatalog.getFunctionDefinitions().values()) {
            if (!safePublicationFilter.test(definition)) {
                continue;
            }

            McpSchema.JsonSchema inputSchema = objectMapper.convertValue(
                    definition.getCanonicalParametersSchema(),
                    McpSchema.JsonSchema.class
            );
            McpSchema.Tool tool = McpSchema.Tool.builder()
                    .name(definition.getName())
                    .description(definition.getDescription())
                    .inputSchema(inputSchema)
                    .build();
            specifications.add(new SyncToolSpecification(
                    tool,
                    (exchange, request) -> invoke(
                            safeCatalog,
                            request.name(),
                            request.arguments()
                    )
            ));
        }
        return List.copyOf(specifications);
    }

    private McpSchema.CallToolResult invoke(
            AIFunctionCatalog catalog,
            String functionName,
            Map<String, Object> arguments
    ) {
        JsonNode argumentsNode = objectMapper.valueToTree(arguments == null ? Map.of() : arguments);
        AIFunctionInvocationResult result = catalog.invokeResult(functionName, argumentsNode);
        return new McpSchema.CallToolResult(
                List.of(new McpSchema.TextContent(writeJson(result.getPayload()))),
                result.isError(),
                objectMapper.convertValue(result.getPayload(), Object.class),
                null
        );
    }

    private String writeJson(JsonNode payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to serialize MCP payload.", exception);
        }
    }
}

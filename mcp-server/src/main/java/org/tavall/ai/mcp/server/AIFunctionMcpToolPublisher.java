package org.tavall.ai.mcp.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema;
import org.tavall.ai.core.catalog.AIFunctionCatalog;
import org.tavall.ai.core.catalog.AIFunctionCatalogView;
import org.tavall.ai.core.catalog.AIFunctionDefinition;
import org.tavall.ai.core.catalog.AIFunctionPublicationDefinition;
import org.tavall.ai.core.invocation.AIFunctionInvocationResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

/** Publishes canonical Function Catalog definitions as MCP tool specifications. */
public final class AIFunctionMcpToolPublisher {
    private final ObjectMapper objectMapper;

    public AIFunctionMcpToolPublisher(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    public List<SyncToolSpecification> toolSpecifications(AIFunctionCatalog catalog) {
        return viewToolSpecifications(new AIFunctionCatalogView(
                Objects.requireNonNull(catalog, "catalog"),
                ignored -> true
        ));
    }

    /** Preserves the pre-view predicate API for existing trusted in-process publishers. */
    public List<SyncToolSpecification> toolSpecifications(
            AIFunctionCatalog catalog,
            Predicate<AIFunctionDefinition> publicationFilter
    ) {
        AIFunctionCatalog safeCatalog = Objects.requireNonNull(catalog, "catalog");
        Predicate<AIFunctionDefinition> safeFilter = Objects.requireNonNull(
                publicationFilter,
                "publicationFilter"
        );
        return viewToolSpecifications(new AIFunctionCatalogView(
                safeCatalog,
                publication -> {
                    AIFunctionDefinition current = safeCatalog.getFunctionDefinitions().get(publication.getName());
                    return current != null && safeFilter.test(current);
                }
        ));
    }

    public List<SyncToolSpecification> viewToolSpecifications(AIFunctionCatalogView catalogView) {
        AIFunctionCatalogView safeCatalogView = Objects.requireNonNull(catalogView, "catalogView");
        List<SyncToolSpecification> specifications = new ArrayList<>();
        for (AIFunctionPublicationDefinition definition : safeCatalogView.getFunctionDefinitions().values()) {
            String publishedFunctionName = definition.getName();
            McpSchema.JsonSchema inputSchema = objectMapper.convertValue(
                    definition.getCanonicalParametersSchema(),
                    McpSchema.JsonSchema.class
            );
            McpSchema.Tool tool = McpSchema.Tool.builder()
                    .name(publishedFunctionName)
                    .description(definition.getDescription())
                    .inputSchema(inputSchema)
                    .build();
            specifications.add(new SyncToolSpecification(
                    tool,
                    (exchange, request) -> invoke(
                            safeCatalogView,
                            publishedFunctionName,
                            request.arguments()
                    )
            ));
        }
        return List.copyOf(specifications);
    }

    private McpSchema.CallToolResult invoke(
            AIFunctionCatalogView catalogView,
            String functionName,
            Map<String, Object> arguments
    ) {
        JsonNode argumentsNode = objectMapper.valueToTree(arguments == null ? Map.of() : arguments);
        AIFunctionInvocationResult result = catalogView.invokeResult(functionName, argumentsNode);
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

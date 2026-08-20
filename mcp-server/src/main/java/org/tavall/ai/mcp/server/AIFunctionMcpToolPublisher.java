package org.tavall.ai.mcp.server;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema;
import org.tavall.ai.core.catalog.AIFunctionCatalog;
import org.tavall.ai.core.catalog.AIFunctionCatalogView;
import org.tavall.ai.core.catalog.AIFunctionDefinition;
import org.tavall.ai.core.catalog.AIFunctionPublicationDefinition;
import org.tavall.ai.core.invocation.AIFunctionInvocationResult;
import org.tavall.ai.core.invocation.AIFunctionOutput;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

/** Publishes canonical Function Catalog definitions as MCP tool specifications. */
public final class AIFunctionMcpToolPublisher {
    private static final TypeReference<Map<String, Object>> SCHEMA_MAP_TYPE = new TypeReference<>() { };

    private final ObjectMapper objectMapper;

    public AIFunctionMcpToolPublisher(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    public List<SyncToolSpecification> toolSpecifications(AIFunctionCatalog catalog) {
        return toolSpecifications(catalog, ignored -> true);
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
        List<SyncToolSpecification> specifications = new ArrayList<>();
        for (AIFunctionDefinition definition : safeCatalog.getFunctionDefinitions().values()) {
            if (!safeFilter.test(definition)) {
                continue;
            }
            String publishedFunctionName = definition.getName();
            specifications.add(specification(
                    publishedFunctionName,
                    definition.getDescription(),
                    definition.getCanonicalParametersSchema(),
                    definition.getCanonicalOutputSchema(),
                    arguments -> safeCatalog.invokeResult(publishedFunctionName, arguments)
            ));
        }
        return List.copyOf(specifications);
    }

    /** Publishes an already-authorized metadata-only capability view. */
    public List<SyncToolSpecification> viewToolSpecifications(AIFunctionCatalogView catalogView) {
        AIFunctionCatalogView safeCatalogView = Objects.requireNonNull(catalogView, "catalogView");
        List<SyncToolSpecification> specifications = new ArrayList<>();
        for (AIFunctionPublicationDefinition definition : safeCatalogView.getFunctionDefinitions().values()) {
            String publishedFunctionName = definition.getName();
            specifications.add(specification(
                    publishedFunctionName,
                    definition.getDescription(),
                    definition.getCanonicalParametersSchema(),
                    definition.getCanonicalOutputSchema(),
                    arguments -> safeCatalogView.invokeResult(publishedFunctionName, arguments)
            ));
        }
        return List.copyOf(specifications);
    }

    private SyncToolSpecification specification(
            String functionName,
            String description,
            JsonNode canonicalParametersSchema,
            JsonNode canonicalOutputSchema,
            Invocation invocation
    ) {
        McpSchema.JsonSchema inputSchema = objectMapper.convertValue(
                canonicalParametersSchema,
                McpSchema.JsonSchema.class
        );
        McpSchema.Tool.Builder toolBuilder = McpSchema.Tool.builder()
                .name(functionName)
                .description(description)
                .inputSchema(inputSchema);
        if (canonicalOutputSchema != null) {
            toolBuilder.outputSchema(objectMapper.convertValue(canonicalOutputSchema, SCHEMA_MAP_TYPE));
        }
        McpSchema.Tool tool = toolBuilder.build();
        return new SyncToolSpecification(
                tool,
                (exchange, request) -> result(invocation.invoke(
                        objectMapper.valueToTree(request.arguments() == null ? Map.of() : request.arguments())
                ))
        );
    }

    private McpSchema.CallToolResult result(AIFunctionInvocationResult result) {
        JsonNode invocationPayload = result.getPayload();
        JsonNode structuredPayload = invocationPayload;
        List<McpSchema.Content> content = new ArrayList<>();

        if (isRichOutput(invocationPayload)) {
            structuredPayload = invocationPayload.path("payload");
            JsonNode richContents = invocationPayload.path("contents");
            if (!richContents.isArray()) {
                throw new IllegalStateException("Rich function output contents must be an array.");
            }
            for (JsonNode richContent : richContents) {
                content.add(toMcpContent(richContent));
            }
        }

        content.addFirst(new McpSchema.TextContent(writeJson(structuredPayload)));
        return new McpSchema.CallToolResult(
                List.copyOf(content),
                result.isError(),
                objectMapper.convertValue(structuredPayload, Object.class),
                null
        );
    }

    private boolean isRichOutput(JsonNode payload) {
        return payload != null
                && payload.isObject()
                && AIFunctionOutput.OUTPUT_TYPE.equals(payload.path("outputType").asText());
    }

    private McpSchema.Content toMcpContent(JsonNode content) {
        String type = requiredText(content, "type");
        String data = requiredText(content, "data");
        String mimeType = requiredText(content, "mimeType");
        return switch (type) {
            case "image" -> {
                if (!mimeType.startsWith("image/")) {
                    throw new IllegalStateException("Rich image content must use an image MIME type.");
                }
                yield new McpSchema.ImageContent(null, data, mimeType);
            }
            case "resource" -> new McpSchema.EmbeddedResource(
                    null,
                    new McpSchema.BlobResourceContents(
                            requiredText(content, "uri"),
                            mimeType,
                            data
                    )
            );
            default -> throw new IllegalStateException("Unsupported rich function content type: " + type);
        };
    }

    private String requiredText(JsonNode node, String fieldName) {
        String value = node.path(fieldName).asText("").strip();
        if (value.isEmpty()) {
            throw new IllegalStateException("Rich function content is missing " + fieldName + ".");
        }
        return value;
    }

    private String writeJson(JsonNode payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to serialize MCP payload.", exception);
        }
    }

    @FunctionalInterface
    private interface Invocation {
        AIFunctionInvocationResult invoke(JsonNode arguments);
    }
}

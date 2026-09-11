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
import org.tavall.ai.core.invocation.AIFunctionOutput;

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
        return toolSpecifications(catalog, ignored -> true, Map.of());
    }

    /** Publishes canonical tools while layering optional MCP projection metadata by function name. */
    public List<SyncToolSpecification> toolSpecifications(
            AIFunctionCatalog catalog,
            Map<String, ToolPresentation> presentations
    ) {
        return toolSpecifications(catalog, ignored -> true, presentations);
    }

    /** Preserves the pre-view predicate API for existing trusted in-process publishers. */
    public List<SyncToolSpecification> toolSpecifications(
            AIFunctionCatalog catalog,
            Predicate<AIFunctionDefinition> publicationFilter
    ) {
        return toolSpecifications(catalog, publicationFilter, Map.of());
    }

    /** Publishes filtered canonical tools with MCP projection metadata that cannot alter execution. */
    public List<SyncToolSpecification> toolSpecifications(
            AIFunctionCatalog catalog,
            Predicate<AIFunctionDefinition> publicationFilter,
            Map<String, ToolPresentation> presentations
    ) {
        AIFunctionCatalog safeCatalog = Objects.requireNonNull(catalog, "catalog");
        Predicate<AIFunctionDefinition> safeFilter = Objects.requireNonNull(
                publicationFilter,
                "publicationFilter"
        );
        Map<String, ToolPresentation> safePresentations = Map.copyOf(
                Objects.requireNonNull(presentations, "presentations")
        );
        List<SyncToolSpecification> specifications = new ArrayList<>();
        for (AIFunctionDefinition definition : safeCatalog.getFunctionDefinitions().values()) {
            if (!safeFilter.test(definition)) {
                continue;
            }
            String publishedFunctionName = definition.getName();
            ToolPresentation presentation = safePresentations.get(publishedFunctionName);
            specifications.add(specification(
                    publishedFunctionName,
                    presentation == null ? null : presentation.title(),
                    definition.getDescription(),
                    definition.getCanonicalParametersSchema(),
                    presentation == null ? Map.of() : presentation.meta(),
                    presentation == null ? "" : presentation.textContentJsonPointer(),
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
                    null,
                    definition.getDescription(),
                    definition.getCanonicalParametersSchema(),
                    Map.of(),
                    "",
                    arguments -> safeCatalogView.invokeResult(publishedFunctionName, arguments)
            ));
        }
        return List.copyOf(specifications);
    }

    private SyncToolSpecification specification(
            String functionName,
            String title,
            String description,
            JsonNode canonicalParametersSchema,
            Map<String, Object> meta,
            String textContentJsonPointer,
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
        if (title != null && !title.isBlank()) {
            toolBuilder.title(title);
        }
        if (!meta.isEmpty()) {
            toolBuilder.meta(meta);
        }
        McpSchema.Tool tool = toolBuilder.build();
        return new SyncToolSpecification(
                tool,
                (exchange, request) -> result(
                        invocation.invoke(
                                objectMapper.valueToTree(request.arguments() == null ? Map.of() : request.arguments())
                        ),
                        textContentJsonPointer
                )
        );
    }

    private McpSchema.CallToolResult result(
            AIFunctionInvocationResult result,
            String textContentJsonPointer
    ) {
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

        JsonNode textPayload = resolveTextPayload(structuredPayload, textContentJsonPointer);
        content.addFirst(new McpSchema.TextContent(writeJson(textPayload)));
        return new McpSchema.CallToolResult(
                List.copyOf(content),
                result.isError(),
                objectMapper.convertValue(structuredPayload, Object.class),
                null
        );
    }

    private JsonNode resolveTextPayload(JsonNode structuredPayload, String jsonPointer) {
        if (jsonPointer == null || jsonPointer.isBlank()) {
            return structuredPayload;
        }
        JsonNode projected = structuredPayload.at(jsonPointer);
        if (projected.isMissingNode()) {
            throw new IllegalStateException("Configured MCP text-content JSON pointer did not resolve: " + jsonPointer);
        }
        return projected;
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

    /** Human-facing presentation plus optional text-content projection over canonical execution. */
    public record ToolPresentation(
            String title,
            Map<String, Object> meta,
            String textContentJsonPointer
    ) {
        public ToolPresentation(String title, Map<String, Object> meta) {
            this(title, meta, "");
        }

        public ToolPresentation {
            title = title == null ? "" : title;
            meta = meta == null ? Map.of() : Map.copyOf(meta);
            textContentJsonPointer = textContentJsonPointer == null ? "" : textContentJsonPointer.trim();
            if (!textContentJsonPointer.isEmpty() && !textContentJsonPointer.startsWith("/")) {
                throw new IllegalArgumentException("textContentJsonPointer must be blank or an absolute JSON pointer");
            }
        }
    }

    @FunctionalInterface
    private interface Invocation {
        AIFunctionInvocationResult invoke(JsonNode arguments);
    }
}

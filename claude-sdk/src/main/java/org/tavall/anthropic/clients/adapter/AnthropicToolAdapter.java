package org.tavall.anthropic.clients.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.tavall.ai.core.catalog.AIFunctionCatalog;
import org.tavall.ai.core.catalog.AIFunctionDefinition;

public final class AnthropicToolAdapter {
    private final ObjectMapper objectMapper;

    public AnthropicToolAdapter(ObjectMapper objectMapper) {
        this.objectMapper = requireValue(objectMapper, "objectMapper");
    }

    public ArrayNode toAnthropicTools(AIFunctionCatalog functionCatalog) {
        AIFunctionCatalog safeFunctionCatalog = requireValue(functionCatalog, "functionCatalog");
        ArrayNode tools = objectMapper.createArrayNode();

        for (AIFunctionDefinition definition : safeFunctionCatalog.getFunctionDefinitions().values()) {
            ObjectNode tool = objectMapper.createObjectNode();
            tool.put("name", definition.getName());
            tool.put("description", definition.getDescription());
            tool.set("input_schema", definition.getCanonicalParametersSchema());
            tools.add(tool);
        }

        return tools;
    }

    private static <T> T requireValue(T value, String fieldName) {
        if (value != null) {
            return value;
        }

        throw new IllegalArgumentException(fieldName + " must not be null");
    }
}

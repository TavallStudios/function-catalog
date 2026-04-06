package org.tavall.anthropic.clients.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.tavall.ai.core.invocation.AIFunctionCall;

import java.util.ArrayList;
import java.util.List;

public final class AnthropicToolCallParser {
    private final ObjectMapper objectMapper;

    public AnthropicToolCallParser(ObjectMapper objectMapper) {
        this.objectMapper = requireValue(objectMapper, "objectMapper");
    }

    public List<AIFunctionCall> parseToolCalls(JsonNode response) {
        JsonNode safeResponse = requireValue(response, "response");
        List<AIFunctionCall> functionCalls = new ArrayList<>();
        JsonNode contentNode = safeResponse.path("content");
        if (contentNode == null || !contentNode.isArray()) {
            return functionCalls;
        }

        for (JsonNode block : contentNode) {
            if ("tool_use".equals(block.path("type").asText())) {
                functionCalls.add(parseToolUseBlock(block));
            }
        }
        return functionCalls;
    }

    private AIFunctionCall parseToolUseBlock(JsonNode block) {
        String id = readText(block, "id");
        String name = readText(block, "name");
        if (name == null) {
            throw new IllegalArgumentException("Anthropic tool_use block is missing name.");
        }
        return new AIFunctionCall(id, name, parseInput(block.path("input")));
    }

    private JsonNode parseInput(JsonNode inputNode) {
        if (inputNode != null && inputNode.isObject()) {
            return inputNode.deepCopy();
        }
        if (inputNode != null && inputNode.isTextual()) {
            try {
                JsonNode parsed = objectMapper.readTree(inputNode.asText());
                if (parsed.isObject()) {
                    return parsed;
                }
            } catch (Exception exception) {
                throw new IllegalArgumentException("Failed to parse Anthropic tool input.", exception);
            }
        }
        return objectMapper.createObjectNode();
    }

    private String readText(JsonNode node, String fieldName) {
        if (node != null && node.path(fieldName).isTextual()) {
            String text = node.path(fieldName).asText();
            if (!text.isBlank()) {
                return text;
            }
        }
        return null;
    }

    private static <T> T requireValue(T value, String fieldName) {
        if (value != null) {
            return value;
        }
        throw new IllegalArgumentException(fieldName + " must not be null");
    }
}

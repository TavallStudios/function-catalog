package org.tavall.openai.clients.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.tavall.ai.core.invocation.AIFunctionCall;

import java.util.ArrayList;
import java.util.List;

public final class OpenAIFunctionCallParser {
    private final ObjectMapper objectMapper;

    public OpenAIFunctionCallParser(ObjectMapper objectMapper) {
        this.objectMapper = requireValue(objectMapper, "objectMapper");
    }

    public List<AIFunctionCall> parseToolCalls(JsonNode response) {
        JsonNode safeResponse = requireValue(response, "response");
        List<AIFunctionCall> functionCalls = new ArrayList<>();
        collectToolCalls(safeResponse.path("tool_calls"), functionCalls);
        collectFromChoices(safeResponse.path("choices"), functionCalls);
        collectFromResponseOutput(safeResponse.path("output"), functionCalls);
        return functionCalls;
    }

    private void collectFromChoices(JsonNode choicesNode, List<AIFunctionCall> functionCalls) {
        if (choicesNode == null || !choicesNode.isArray()) {
            return;
        }
        for (JsonNode choiceNode : choicesNode) {
            collectToolCalls(choiceNode.path("message").path("tool_calls"), functionCalls);
        }
    }

    private void collectFromResponseOutput(JsonNode outputNode, List<AIFunctionCall> functionCalls) {
        if (outputNode == null || !outputNode.isArray()) {
            return;
        }
        for (JsonNode outputItem : outputNode) {
            if ("function_call".equals(outputItem.path("type").asText())) {
                functionCalls.add(parseSingleToolCall(outputItem));
            }
        }
    }

    private void collectToolCalls(JsonNode toolCallsNode, List<AIFunctionCall> functionCalls) {
        if (toolCallsNode == null || !toolCallsNode.isArray()) {
            return;
        }
        for (JsonNode toolCallNode : toolCallsNode) {
            functionCalls.add(parseSingleToolCall(toolCallNode));
        }
    }

    private AIFunctionCall parseSingleToolCall(JsonNode toolCallNode) {
        String callId = readOptionalText(toolCallNode, "id");
        JsonNode functionNode = toolCallNode.path("function");
        String name = readText(functionNode, "name");
        if (name == null) {
            name = readText(toolCallNode, "name");
        }
        if (name == null) {
            throw new IllegalArgumentException("OpenAI tool call is missing function name.");
        }

        JsonNode argumentsNode = parseArguments(functionNode.path("arguments"));
        if (argumentsNode == null) {
            argumentsNode = parseArguments(toolCallNode.path("arguments"));
        }
        if (argumentsNode == null) {
            argumentsNode = objectMapper.createObjectNode();
        }
        return new AIFunctionCall(callId, name, argumentsNode);
    }

    private JsonNode parseArguments(JsonNode rawArguments) {
        if (rawArguments != null && rawArguments.isObject()) {
            return rawArguments.deepCopy();
        }
        if (rawArguments != null && rawArguments.isTextual()) {
            String argumentsText = rawArguments.asText();
            if (argumentsText == null || argumentsText.isBlank()) {
                return objectMapper.createObjectNode();
            }
            try {
                JsonNode parsed = objectMapper.readTree(argumentsText);
                if (parsed.isObject()) {
                    return parsed;
                }
            } catch (Exception exception) {
                throw new IllegalArgumentException("Failed to parse OpenAI function arguments.", exception);
            }
        }
        return null;
    }

    private String readOptionalText(JsonNode node, String fieldName) {
        return readText(node, fieldName);
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

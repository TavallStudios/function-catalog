package org.tavall.openai.clients;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.tavall.ai.core.catalog.AIFunctionCatalog;
import org.tavall.ai.core.invocation.AIFunctionCall;
import org.tavall.ai.core.invocation.AIFunctionInvocationResult;
import org.tavall.ai.core.invocation.AIFunctionInvocationRouter;
import org.tavall.openai.clients.adapter.OpenAIFunctionAdapter;
import org.tavall.openai.clients.parser.OpenAIFunctionCallParser;
import org.tavall.openai.clients.response.OpenAIFunctionResultMessage;

import java.util.ArrayList;
import java.util.List;

public final class OpenAIClientWrapper {
    private final ObjectMapper objectMapper;
    private final AIFunctionCatalog functionCatalog;
    private final AIFunctionInvocationRouter invocationRouter;
    private final OpenAIFunctionAdapter functionAdapter;
    private final OpenAIFunctionCallParser functionCallParser;

    public OpenAIClientWrapper(
            ObjectMapper objectMapper,
            AIFunctionCatalog functionCatalog,
            OpenAIFunctionAdapter functionAdapter,
            OpenAIFunctionCallParser functionCallParser
    ) {
        this.objectMapper = requireValue(objectMapper, "objectMapper");
        this.functionCatalog = requireValue(functionCatalog, "functionCatalog");
        this.invocationRouter = new AIFunctionInvocationRouter(functionCatalog);
        this.functionAdapter = requireValue(functionAdapter, "functionAdapter");
        this.functionCallParser = requireValue(functionCallParser, "functionCallParser");
    }

    public ArrayNode buildToolPayload() {
        return functionAdapter.toOpenAITools(functionCatalog);
    }

    public List<AIFunctionCall> parseToolCalls(JsonNode response) {
        return functionCallParser.parseToolCalls(response);
    }

    public List<OpenAIFunctionResultMessage> executeToolCalls(JsonNode response) {
        return executeToolCalls(parseToolCalls(response));
    }

    public List<OpenAIFunctionResultMessage> executeToolCalls(List<AIFunctionCall> functionCalls) {
        List<AIFunctionCall> safeFunctionCalls = requireValue(functionCalls, "functionCalls");
        List<OpenAIFunctionResultMessage> results = new ArrayList<>();

        for (AIFunctionCall functionCall : safeFunctionCalls) {
            AIFunctionInvocationResult invocationResult = invocationRouter.invoke(functionCall);
            JsonNode payload = invocationResult.getPayload();
            results.add(new OpenAIFunctionResultMessage(
                    functionCall.getCallId(),
                    functionCall.getFunctionName(),
                    serialize(payload),
                    payload,
                    invocationResult.isError(),
                    invocationResult.getErrorCode(),
                    invocationResult.getErrorMessage()
            ));
        }

        return results;
    }

    public ArrayNode toToolMessagePayload(List<OpenAIFunctionResultMessage> results) {
        List<OpenAIFunctionResultMessage> safeResults = requireValue(results, "results");
        ArrayNode messages = objectMapper.createArrayNode();

        for (OpenAIFunctionResultMessage result : safeResults) {
            ObjectNode message = objectMapper.createObjectNode();
            message.put("role", "tool");
            message.put("name", result.getFunctionName());
            message.put("content", result.getContent());
            if (result.getToolCallId() != null && !result.getToolCallId().isBlank()) {
                message.put("tool_call_id", result.getToolCallId());
            }
            messages.add(message);
        }

        return messages;
    }

    private String serialize(JsonNode payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to serialize OpenAI function result.", exception);
        }
    }

    private static <T> T requireValue(T value, String fieldName) {
        if (value != null) {
            return value;
        }

        throw new IllegalArgumentException(fieldName + " must not be null");
    }
}

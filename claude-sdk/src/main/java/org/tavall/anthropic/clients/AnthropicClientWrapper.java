package org.tavall.anthropic.clients;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.tavall.ai.core.catalog.AIFunctionCatalog;
import org.tavall.ai.core.invocation.AIFunctionCall;
import org.tavall.ai.core.invocation.AIFunctionInvocationResult;
import org.tavall.ai.core.invocation.AIFunctionInvocationRouter;
import org.tavall.anthropic.clients.adapter.AnthropicToolAdapter;
import org.tavall.anthropic.clients.parser.AnthropicToolCallParser;
import org.tavall.anthropic.clients.response.AnthropicToolResultBlock;

import java.util.ArrayList;
import java.util.List;

public final class AnthropicClientWrapper {
    private final ObjectMapper objectMapper;
    private final AIFunctionCatalog functionCatalog;
    private final AIFunctionInvocationRouter invocationRouter;
    private final AnthropicToolAdapter toolAdapter;
    private final AnthropicToolCallParser toolCallParser;

    public AnthropicClientWrapper(
            ObjectMapper objectMapper,
            AIFunctionCatalog functionCatalog,
            AnthropicToolAdapter toolAdapter,
            AnthropicToolCallParser toolCallParser
    ) {
        this.objectMapper = requireValue(objectMapper, "objectMapper");
        this.functionCatalog = requireValue(functionCatalog, "functionCatalog");
        this.invocationRouter = new AIFunctionInvocationRouter(functionCatalog);
        this.toolAdapter = requireValue(toolAdapter, "toolAdapter");
        this.toolCallParser = requireValue(toolCallParser, "toolCallParser");
    }

    public ArrayNode buildToolPayload() {
        return toolAdapter.toAnthropicTools(functionCatalog);
    }

    public List<AIFunctionCall> parseToolCalls(JsonNode response) {
        return toolCallParser.parseToolCalls(response);
    }

    public List<AnthropicToolResultBlock> executeToolCalls(JsonNode response) {
        return executeToolCalls(parseToolCalls(response));
    }

    public List<AnthropicToolResultBlock> executeToolCalls(List<AIFunctionCall> functionCalls) {
        List<AIFunctionCall> safeFunctionCalls = requireValue(functionCalls, "functionCalls");
        List<AnthropicToolResultBlock> resultBlocks = new ArrayList<>();

        for (AIFunctionCall functionCall : safeFunctionCalls) {
            AIFunctionInvocationResult invocationResult = invocationRouter.invoke(functionCall);
            JsonNode payload = invocationResult.getPayload();
            resultBlocks.add(new AnthropicToolResultBlock(
                    functionCall.getCallId(),
                    functionCall.getFunctionName(),
                    serialize(payload),
                    payload,
                    invocationResult.isError(),
                    invocationResult.getErrorCode(),
                    invocationResult.getErrorMessage()
            ));
        }

        return resultBlocks;
    }

    public ArrayNode toToolResultContent(List<AnthropicToolResultBlock> resultBlocks) {
        List<AnthropicToolResultBlock> safeBlocks = requireValue(resultBlocks, "resultBlocks");
        ArrayNode content = objectMapper.createArrayNode();

        for (AnthropicToolResultBlock resultBlock : safeBlocks) {
            ObjectNode block = objectMapper.createObjectNode();
            block.put("type", "tool_result");
            block.put("content", resultBlock.getContent());
            if (resultBlock.getToolUseId() != null && !resultBlock.getToolUseId().isBlank()) {
                block.put("tool_use_id", resultBlock.getToolUseId());
            }
            if (resultBlock.isError()) {
                block.put("is_error", true);
            }
            content.add(block);
        }

        return content;
    }

    private String serialize(JsonNode payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to serialize Anthropic function result.", exception);
        }
    }

    private static <T> T requireValue(T value, String fieldName) {
        if (value != null) {
            return value;
        }

        throw new IllegalArgumentException(fieldName + " must not be null");
    }
}

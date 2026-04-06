package org.tavall.anthropic.clients.response;

import com.fasterxml.jackson.databind.JsonNode;

public final class AnthropicToolResultBlock {
    private final String toolUseId;
    private final String functionName;
    private final String content;
    private final JsonNode result;
    private final boolean error;
    private final String errorCode;
    private final String errorMessage;

    public AnthropicToolResultBlock(
            String toolUseId,
            String functionName,
            String content,
            JsonNode result,
            boolean error,
            String errorCode,
            String errorMessage
    ) {
        this.toolUseId = toolUseId;
        this.functionName = requireText(functionName, "functionName");
        this.content = requireText(content, "content");
        this.result = requireValue(result, "result").deepCopy();
        this.error = error;
        this.errorCode = errorCode == null ? "" : errorCode;
        this.errorMessage = errorMessage == null ? "" : errorMessage;
    }

    public String getToolUseId() {
        return toolUseId;
    }

    public String getFunctionName() {
        return functionName;
    }

    public String getContent() {
        return content;
    }

    public JsonNode getResult() {
        return result.deepCopy();
    }

    public boolean isError() {
        return error;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    private static <T> T requireValue(T value, String fieldName) {
        if (value != null) {
            return value;
        }

        throw new IllegalArgumentException(fieldName + " must not be null");
    }

    private static String requireText(String value, String fieldName) {
        if (value != null && !value.isBlank()) {
            return value;
        }

        throw new IllegalArgumentException(fieldName + " must not be blank");
    }
}

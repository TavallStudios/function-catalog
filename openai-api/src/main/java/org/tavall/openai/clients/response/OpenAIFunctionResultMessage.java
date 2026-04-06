package org.tavall.openai.clients.response;

import com.fasterxml.jackson.databind.JsonNode;

public final class OpenAIFunctionResultMessage {
    private final String toolCallId;
    private final String functionName;
    private final String content;
    private final JsonNode result;
    private final boolean error;
    private final String errorCode;
    private final String errorMessage;

    public OpenAIFunctionResultMessage(
            String toolCallId,
            String functionName,
            String content,
            JsonNode result,
            boolean error,
            String errorCode,
            String errorMessage
    ) {
        this.toolCallId = toolCallId;
        this.functionName = requireText(functionName, "functionName");
        this.content = requireText(content, "content");
        this.result = requireValue(result, "result").deepCopy();
        this.error = error;
        this.errorCode = errorCode == null ? "" : errorCode;
        this.errorMessage = errorMessage == null ? "" : errorMessage;
    }

    public String getToolCallId() {
        return toolCallId;
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

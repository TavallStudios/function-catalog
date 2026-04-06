package org.tavall.ai.core.invocation;

import com.fasterxml.jackson.databind.JsonNode;

public final class AIFunctionInvocationResult {
    private final String callId;
    private final String functionName;
    private final JsonNode arguments;
    private final boolean success;
    private final String errorCode;
    private final String errorMessage;
    private final JsonNode payload;

    public AIFunctionInvocationResult(
            String callId,
            String functionName,
            JsonNode arguments,
            boolean success,
            String errorCode,
            String errorMessage,
            JsonNode payload
    ) {
        this.callId = callId;
        this.functionName = requireText(functionName, "functionName");
        this.arguments = requireValue(arguments, "arguments").deepCopy();
        this.success = success;
        this.errorCode = normalize(errorCode);
        this.errorMessage = normalize(errorMessage);
        this.payload = requireValue(payload, "payload").deepCopy();
    }

    public String getCallId() {
        return callId;
    }

    public String getFunctionName() {
        return functionName;
    }

    public JsonNode getArguments() {
        return arguments.deepCopy();
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean isError() {
        return !success;
    }

    public boolean isDisabled() {
        return "disabled".equals(errorCode);
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public JsonNode getPayload() {
        return payload.deepCopy();
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

    private static String normalize(String value) {
        return value == null ? "" : value;
    }
}

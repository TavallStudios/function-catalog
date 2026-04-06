package org.tavall.ai.core.invocation;

import com.fasterxml.jackson.databind.JsonNode;

public final class AIFunctionCall {
    private final String callId;
    private final String functionName;
    private final JsonNode arguments;

    public AIFunctionCall(String functionName, JsonNode arguments) {
        this(null, functionName, arguments);
    }

    public AIFunctionCall(String callId, String functionName, JsonNode arguments) {
        this.callId = callId;
        this.functionName = requireText(functionName, "functionName");
        this.arguments = requireValue(arguments, "arguments").deepCopy();
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

package org.tavall.ai.core.policy;

import com.fasterxml.jackson.databind.JsonNode;
import org.tavall.ai.core.catalog.AIFunctionDefinition;

public final class AIFunctionInvocationContext {
    private final String functionName;
    private final JsonNode arguments;
    private final AIFunctionDefinition functionDefinition;

    public AIFunctionInvocationContext(String functionName, JsonNode arguments, AIFunctionDefinition functionDefinition) {
        this.functionName = requireText(functionName, "functionName");
        this.arguments = requireValue(arguments, "arguments");
        this.functionDefinition = requireValue(functionDefinition, "functionDefinition");
    }

    public String getFunctionName() {
        return functionName;
    }

    public JsonNode getArguments() {
        return arguments;
    }

    public AIFunctionDefinition getFunctionDefinition() {
        return functionDefinition;
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

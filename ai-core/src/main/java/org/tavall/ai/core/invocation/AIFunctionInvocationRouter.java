package org.tavall.ai.core.invocation;

import com.fasterxml.jackson.databind.JsonNode;
import org.tavall.ai.core.catalog.AIFunctionCatalog;

import java.util.ArrayList;
import java.util.List;

public final class AIFunctionInvocationRouter {
    private final AIFunctionCatalog functionCatalog;

    public AIFunctionInvocationRouter(AIFunctionCatalog functionCatalog) {
        this.functionCatalog = requireValue(functionCatalog, "functionCatalog");
    }

    public AIFunctionInvocationResult invoke(AIFunctionCall functionCall) {
        AIFunctionCall safeFunctionCall = requireValue(functionCall, "functionCall");
        return functionCatalog.invokeResult(
                safeFunctionCall.getCallId(),
                safeFunctionCall.getFunctionName(),
                safeFunctionCall.getArguments()
        );
    }

    public JsonNode invokeAsJson(AIFunctionCall functionCall) {
        return invoke(functionCall).getPayload();
    }

    public List<AIFunctionInvocationResult> invokeAll(Iterable<AIFunctionCall> functionCalls) {
        Iterable<AIFunctionCall> safeFunctionCalls = requireValue(functionCalls, "functionCalls");
        List<AIFunctionInvocationResult> results = new ArrayList<>();

        for (AIFunctionCall functionCall : safeFunctionCalls) {
            results.add(invoke(functionCall));
        }

        return results;
    }

    public List<JsonNode> invokeAllAsJson(Iterable<AIFunctionCall> functionCalls) {
        List<JsonNode> results = new ArrayList<>();
        for (AIFunctionInvocationResult result : invokeAll(functionCalls)) {
            results.add(result.getPayload());
        }
        return results;
    }

    private static <T> T requireValue(T value, String fieldName) {
        if (value != null) {
            return value;
        }

        throw new IllegalArgumentException(fieldName + " must not be null");
    }
}

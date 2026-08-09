package org.tavall.ai.core.catalog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.tavall.ai.core.invocation.AIFunctionInvocationResult;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

/** Fail-closed invocation-capable view that publishes metadata-only function definitions. */
public final class AIFunctionCatalogView {
    public static final String SCOPE_DENIED_ERROR_CODE = "scope_denied";

    private final AIFunctionCatalog catalog;
    private final Predicate<AIFunctionPublicationDefinition> functionFilter;

    public AIFunctionCatalogView(
            AIFunctionCatalog catalog,
            Predicate<AIFunctionPublicationDefinition> functionFilter
    ) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
        this.functionFilter = Objects.requireNonNull(functionFilter, "functionFilter");
    }

    public Map<String, AIFunctionPublicationDefinition> getFunctionDefinitions() {
        synchronized (catalog) {
            Map<String, AIFunctionPublicationDefinition> definitions = new LinkedHashMap<>();
            for (Map.Entry<String, AIFunctionDefinition> entry : catalog.getFunctionDefinitions().entrySet()) {
                AIFunctionPublicationDefinition publication = publication(entry.getValue());
                if (functionFilter.test(publication)) {
                    definitions.put(entry.getKey(), publication);
                }
            }
            return Collections.unmodifiableMap(definitions);
        }
    }

    public boolean allows(String functionName) {
        String safeFunctionName = requireText(functionName, "functionName");
        synchronized (catalog) {
            AIFunctionDefinition definition = catalog.getFunctionDefinitions().get(safeFunctionName);
            return definition != null && functionFilter.test(publication(definition));
        }
    }

    public AIFunctionInvocationResult invokeResult(String functionName, JsonNode argumentsJson) {
        return invokeResult(null, functionName, argumentsJson);
    }

    public AIFunctionInvocationResult invokeResult(
            String callId,
            String functionName,
            JsonNode argumentsJson
    ) {
        String safeFunctionName = requireText(functionName, "functionName");
        JsonNode safeArguments = argumentsJson == null
                ? JsonNodeFactory.instance.objectNode()
                : argumentsJson.deepCopy();

        synchronized (catalog) {
            AIFunctionDefinition definition = catalog.getFunctionDefinitions().get(safeFunctionName);
            if (definition == null) {
                return catalog.invokeResult(callId, safeFunctionName, safeArguments);
            }

            if (!functionFilter.test(publication(definition))) {
                String message = "Function '" + safeFunctionName + "' is outside this catalog view.";
                ObjectNode payload = JsonNodeFactory.instance.objectNode();
                payload.put("errorCode", SCOPE_DENIED_ERROR_CODE);
                payload.put("message", message);
                return new AIFunctionInvocationResult(
                        callId,
                        safeFunctionName,
                        safeArguments,
                        false,
                        SCOPE_DENIED_ERROR_CODE,
                        message,
                        payload
                );
            }

            return catalog.invokeResult(callId, safeFunctionName, safeArguments);
        }
    }

    private static AIFunctionPublicationDefinition publication(AIFunctionDefinition definition) {
        return AIFunctionPublicationDefinition.from(definition);
    }

    private static String requireText(String value, String fieldName) {
        if (value != null && !value.isBlank()) {
            return value;
        }
        throw new IllegalArgumentException(fieldName + " must not be blank");
    }
}

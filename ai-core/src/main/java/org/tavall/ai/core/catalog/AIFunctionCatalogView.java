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
    public static final String INVOCATION_BUDGET_EXCEEDED_ERROR_CODE = "invocation_budget_exceeded";
    public static final String VIEW_REVOKED_ERROR_CODE = "view_revoked";

    private final AIFunctionCatalog catalog;
    private final Predicate<AIFunctionPublicationDefinition> functionFilter;
    private final InvocationControl invocationControl;

    public AIFunctionCatalogView(
            AIFunctionCatalog catalog,
            Predicate<AIFunctionPublicationDefinition> functionFilter
    ) {
        this(catalog, functionFilter, InvocationControl.unlimited());
    }

    private AIFunctionCatalogView(
            AIFunctionCatalog catalog,
            Predicate<AIFunctionPublicationDefinition> functionFilter,
            InvocationControl invocationControl
    ) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
        this.functionFilter = Objects.requireNonNull(functionFilter, "functionFilter");
        this.invocationControl = Objects.requireNonNull(invocationControl, "invocationControl");
    }

    public Map<String, AIFunctionPublicationDefinition> getFunctionDefinitions() {
        if (invocationControl.isRevoked()) {
            return Map.of();
        }
        synchronized (catalog) {
            if (invocationControl.isRevoked()) {
                return Map.of();
            }
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
        if (invocationControl.isRevoked()) {
            return false;
        }
        String safeFunctionName = requireText(functionName, "functionName");
        synchronized (catalog) {
            if (invocationControl.isRevoked()) {
                return false;
            }
            AIFunctionDefinition definition = catalog.getFunctionDefinitions().get(safeFunctionName);
            return definition != null && functionFilter.test(publication(definition));
        }
    }

    /** Returns a same-catalog view that can only narrow the parent's published function surface. */
    public AIFunctionCatalogView narrow(
            Predicate<AIFunctionPublicationDefinition> additionalFilter
    ) {
        Predicate<AIFunctionPublicationDefinition> safeAdditionalFilter = Objects.requireNonNull(
                additionalFilter,
                "additionalFilter"
        );
        return new AIFunctionCatalogView(
                catalog,
                definition -> functionFilter.test(definition) && safeAdditionalFilter.test(definition),
                invocationControl
        );
    }

    /** Adds a child call limit without replacing any parent call limit or revocation state. */
    public AIFunctionCatalogView withInvocationLimit(int maximumInvocations) {
        if (maximumInvocations < 0) {
            throw new IllegalArgumentException("maximumInvocations must not be negative");
        }
        return new AIFunctionCatalogView(
                catalog,
                functionFilter,
                InvocationControl.limited(invocationControl, maximumInvocations)
        );
    }

    /** Identity check used by runtimes to reject a policy view created over another catalog. */
    public boolean isBackedBy(AIFunctionCatalog expectedCatalog) {
        return catalog == Objects.requireNonNull(expectedCatalog, "expectedCatalog");
    }

    public int invocationCount() {
        return invocationControl.invocationCount();
    }

    /** Revokes this control and every child view that composes it as a parent. */
    public void revoke() {
        invocationControl.revoke();
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

        InvocationDecision invocationDecision = invocationControl.tryAcquire();
        if (invocationDecision != InvocationDecision.ALLOWED) {
            return controlFailure(callId, safeFunctionName, safeArguments, invocationDecision);
        }

        synchronized (catalog) {
            if (invocationControl.isRevoked()) {
                return failure(
                        callId,
                        safeFunctionName,
                        safeArguments,
                        VIEW_REVOKED_ERROR_CODE,
                        "Function Catalog view has been revoked."
                );
            }

            AIFunctionDefinition definition = catalog.getFunctionDefinitions().get(safeFunctionName);
            if (definition == null) {
                return catalog.invokeResult(callId, safeFunctionName, safeArguments);
            }

            if (!functionFilter.test(publication(definition))) {
                return failure(
                        callId,
                        safeFunctionName,
                        safeArguments,
                        SCOPE_DENIED_ERROR_CODE,
                        "Function '" + safeFunctionName + "' is outside this catalog view."
                );
            }

            return catalog.invokeResult(callId, safeFunctionName, safeArguments);
        }
    }

    private AIFunctionInvocationResult controlFailure(
            String callId,
            String functionName,
            JsonNode arguments,
            InvocationDecision decision
    ) {
        if (decision == InvocationDecision.REVOKED) {
            return failure(
                    callId,
                    functionName,
                    arguments,
                    VIEW_REVOKED_ERROR_CODE,
                    "Function Catalog view has been revoked."
            );
        }
        return failure(
                callId,
                functionName,
                arguments,
                INVOCATION_BUDGET_EXCEEDED_ERROR_CODE,
                "Function Catalog invocation budget has been exhausted."
        );
    }

    private AIFunctionInvocationResult failure(
            String callId,
            String functionName,
            JsonNode arguments,
            String errorCode,
            String message
    ) {
        ObjectNode payload = JsonNodeFactory.instance.objectNode();
        payload.put("errorCode", errorCode);
        payload.put("message", message);
        return new AIFunctionInvocationResult(
                callId,
                functionName,
                arguments,
                false,
                errorCode,
                message,
                payload
        );
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

    private enum InvocationDecision {
        ALLOWED,
        BUDGET_EXHAUSTED,
        REVOKED
    }

    private static final class InvocationControl {
        private final InvocationControl parent;
        private final int maximumInvocations;
        private int invocationCount;
        private boolean revoked;

        private InvocationControl(InvocationControl parent, int maximumInvocations) {
            this.parent = parent;
            this.maximumInvocations = maximumInvocations;
        }

        static InvocationControl unlimited() {
            return new InvocationControl(null, -1);
        }

        static InvocationControl limited(InvocationControl parent, int maximumInvocations) {
            return new InvocationControl(
                    Objects.requireNonNull(parent, "parent"),
                    maximumInvocations
            );
        }

        InvocationDecision tryAcquire() {
            synchronized (this) {
                if (revoked) {
                    return InvocationDecision.REVOKED;
                }
                if (maximumInvocations >= 0 && invocationCount >= maximumInvocations) {
                    return InvocationDecision.BUDGET_EXHAUSTED;
                }

                if (parent != null) {
                    InvocationDecision parentDecision = parent.tryAcquire();
                    if (parentDecision != InvocationDecision.ALLOWED) {
                        return parentDecision;
                    }
                }

                if (revoked) {
                    return InvocationDecision.REVOKED;
                }
                invocationCount++;
                return InvocationDecision.ALLOWED;
            }
        }

        synchronized int invocationCount() {
            return invocationCount;
        }

        boolean isRevoked() {
            synchronized (this) {
                if (revoked) {
                    return true;
                }
            }
            return parent != null && parent.isRevoked();
        }

        synchronized void revoke() {
            revoked = true;
        }
    }
}

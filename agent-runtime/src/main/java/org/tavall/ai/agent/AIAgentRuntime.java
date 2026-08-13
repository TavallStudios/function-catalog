package org.tavall.ai.agent;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.tavall.ai.core.catalog.AIFunctionCatalog;
import org.tavall.ai.core.catalog.AIFunctionCatalogView;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Provider-neutral Tavall agent runtime that enforces provider identity, authoritative function
 * views, tool/delegation budgets, and execution timeouts around one backend execution.
 *
 * <p>Providers execute on a dedicated virtual thread for each request. The runtime owns the
 * effective function view and revokes it when execution finishes, fails, times out, or is
 * interrupted, preventing a provider from continuing to invoke functions through the view after its
 * execution boundary has ended.</p>
 */
public final class AIAgentRuntime {
    public static final String PROVIDER_NOT_FOUND = "provider_not_found";
    public static final String BUDGET_EXCEEDED = "budget_exceeded";
    public static final String PROVIDER_FAILURE = "provider_failure";
    public static final String FUNCTION_VIEW_MISMATCH = "function_view_mismatch";
    public static final String EXECUTION_TIMEOUT = "execution_timeout";

    private final AIFunctionCatalog catalog;
    private final AIAgentFunctionViewResolver functionViewResolver;
    private final Map<String, AIAgentProvider> providers;

    /**
     * Creates an agent runtime with a fixed provider registry.
     *
     * <p>Provider identifiers are validated as non-blank and must be unique for the lifetime of this
     * runtime.</p>
     *
     * @param catalog authoritative function catalog used by agent executions
     * @param functionViewResolver resolver that applies environment/job function policy
     * @param providers provider adapters selectable by agent definitions
     * @throws NullPointerException if the catalog, resolver, provider iterable, or an individual
     *                              provider is {@code null}
     * @throws IllegalArgumentException if a provider identifier is blank or duplicated
     */
    public AIAgentRuntime(
            AIFunctionCatalog catalog,
            AIAgentFunctionViewResolver functionViewResolver,
            Iterable<? extends AIAgentProvider> providers
    ) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
        this.functionViewResolver = Objects.requireNonNull(functionViewResolver, "functionViewResolver");
        Objects.requireNonNull(providers, "providers");
        Map<String, AIAgentProvider> byId = new LinkedHashMap<>();
        for (AIAgentProvider provider : providers) {
            AIAgentProvider safeProvider = Objects.requireNonNull(provider, "provider");
            String providerId = requireText(safeProvider.providerId(), "providerId");
            if (byId.putIfAbsent(providerId, safeProvider) != null) {
                throw new IllegalArgumentException("Duplicate AI agent provider id: " + providerId);
            }
        }
        this.providers = Map.copyOf(byId);
    }

    /**
     * Executes one agent job within its declared provider and execution budget.
     *
     * <p>The runtime rejects jobs that already exceed the delegation budget, resolves the requested
     * provider, obtains an authoritative function-policy view, verifies that the view is backed by
     * this runtime's catalog, narrows it to the agent's requested function names, and applies the
     * tool-call limit. Provider execution then runs on a virtual thread under the configured timeout.
     * Provider exceptions, interruption, timeout, function-view mismatch, and budget violations are
     * converted into {@link AIAgentExecutionStatus#FAILED} results rather than escaping as normal
     * execution exceptions.</p>
     *
     * <p>The returned tool-call count is taken from the effective catalog view, not trusted from the
     * provider result. The effective view is revoked in all completion paths.</p>
     *
     * @param definition agent definition selecting the provider and requested functions
     * @param job concrete job to execute
     * @param budget limits for tool calls, delegation depth, and wall-clock execution
     * @return successful provider result normalized with authoritative tool-call accounting, or a
     *         failed execution result carrying one of this runtime's error codes
     * @throws NullPointerException if any argument is {@code null}, or if the resolver/provider
     *                              violates its non-null return contract
     */
    public AIAgentExecutionResult execute(
            AIAgentDefinition definition,
            AIAgentJob job,
            AIAgentExecutionBudget budget
    ) {
        AIAgentDefinition safeDefinition = Objects.requireNonNull(definition, "definition");
        AIAgentJob safeJob = Objects.requireNonNull(job, "job");
        AIAgentExecutionBudget safeBudget = Objects.requireNonNull(budget, "budget");
        if (safeJob.delegationDepth() > safeBudget.maxDelegations()) {
            return failure(BUDGET_EXCEEDED, "Job delegation depth exceeds the execution budget.", 0);
        }

        AIAgentProvider provider = providers.get(safeDefinition.providerId());
        if (provider == null) {
            return failure(PROVIDER_NOT_FOUND, "No AI agent provider registered with id: "
                    + safeDefinition.providerId(), 0);
        }

        AIFunctionCatalogView policyView = Objects.requireNonNull(
                functionViewResolver.resolve(catalog, safeDefinition, safeJob),
                "functionViewResolver result"
        );
        if (!policyView.isBackedBy(catalog)) {
            return failure(FUNCTION_VIEW_MISMATCH,
                    "Authoritative Function Catalog view is backed by a different catalog.", 0);
        }

        AIFunctionCatalogView effectiveView = policyView
                .narrow(function -> safeDefinition.requestedFunctionNames().contains(function.getName()))
                .withInvocationLimit(safeBudget.maxToolCalls());
        AIAgentExecutionRequest request = new AIAgentExecutionRequest(
                safeDefinition,
                safeJob,
                safeBudget,
                effectiveView
        );

        ExecutorService executor = Executors.newThreadPerTaskExecutor(
                Thread.ofVirtual().name("tavall-ai-provider-", 0).factory()
        );
        Future<AIAgentExecutionResult> execution = executor.submit(
                () -> Objects.requireNonNull(provider.execute(request), "provider result")
        );
        try {
            AIAgentExecutionResult providerResult = execution.get(
                    safeBudget.timeout().toNanos(),
                    TimeUnit.NANOSECONDS
            );
            int actualToolCalls = effectiveView.invocationCount();
            if (providerResult.delegatedTasks() > safeBudget.maxDelegations()) {
                return failure(BUDGET_EXCEEDED,
                        "Provider reported delegated tasks above the execution budget.", actualToolCalls);
            }
            return new AIAgentExecutionResult(
                    providerResult.status(),
                    providerResult.output(),
                    actualToolCalls,
                    providerResult.delegatedTasks(),
                    providerResult.errorMessage()
            );
        } catch (TimeoutException exception) {
            effectiveView.revoke();
            execution.cancel(true);
            return failure(EXECUTION_TIMEOUT,
                    "AI agent provider exceeded the execution timeout.", effectiveView.invocationCount());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            effectiveView.revoke();
            execution.cancel(true);
            return failure(PROVIDER_FAILURE,
                    "AI agent execution was interrupted.", effectiveView.invocationCount());
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause() == null ? exception : exception.getCause();
            return failure(PROVIDER_FAILURE, messageFor(cause), effectiveView.invocationCount());
        } catch (ArithmeticException exception) {
            effectiveView.revoke();
            execution.cancel(true);
            return failure(BUDGET_EXCEEDED,
                    "AI agent execution timeout is outside the supported nanosecond range.",
                    effectiveView.invocationCount());
        } finally {
            effectiveView.revoke();
            executor.shutdownNow();
        }
    }

    private static AIAgentExecutionResult failure(String errorCode, String message, int toolCalls) {
        ObjectNode payload = JsonNodeFactory.instance.objectNode();
        payload.put("errorCode", errorCode);
        payload.put("message", message);
        return new AIAgentExecutionResult(AIAgentExecutionStatus.FAILED, payload, toolCalls, 0, message);
    }

    private static String messageFor(Throwable exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }

    private static String requireText(String value, String fieldName) {
        if (value != null && !value.isBlank()) return value;
        throw new IllegalArgumentException(fieldName + " must not be blank");
    }
}

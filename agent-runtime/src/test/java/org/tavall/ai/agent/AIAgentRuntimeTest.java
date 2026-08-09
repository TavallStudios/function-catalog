package org.tavall.ai.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.Test;
import org.tavall.ai.core.annotation.AIFunction;
import org.tavall.ai.core.catalog.AIFunctionCatalog;
import org.tavall.ai.core.catalog.AIFunctionCatalogView;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class AIAgentRuntimeTest {
    @Test
    void intersectsRoleScopeWithAuthoritativePolicyAndUsesObservedToolCount() {
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        AIFunctionCatalog catalog = new AIFunctionCatalog(mapper);
        TestFunctions functions = new TestFunctions();
        catalog.registerInstances(functions);
        AtomicBoolean providerCalled = new AtomicBoolean();
        AIAgentProvider provider = provider("test", providerCalled, request -> {
            assertThat(request.functionView().getFunctionDefinitions()).containsOnlyKeys("safe_read");
            var denied = request.functionView().invokeResult("dangerous_write", mapper.createObjectNode());
            return new AIAgentExecutionResult(AIAgentExecutionStatus.COMPLETED, denied.getPayload(), 999, 0, "");
        });
        AIAgentRuntime runtime = new AIAgentRuntime(
                catalog,
                (root, definition, job) -> new AIFunctionCatalogView(
                        root,
                        function -> function.getName().equals("safe_read")
                ),
                List.of(provider)
        );

        AIAgentExecutionResult result = runtime.execute(
                new AIAgentDefinition("auditor", "", "Inspect safely.", "test",
                        Set.of("safe_read", "dangerous_write")),
                new AIAgentJob("job-1", "inspect", 0, Map.of()),
                new AIAgentExecutionBudget(Duration.ofMinutes(1), 5, 0)
        );

        assertThat(providerCalled).isTrue();
        assertThat(result.toolCalls()).isEqualTo(1);
        assertThat(result.output().path("errorCode").asText())
                .isEqualTo(AIFunctionCatalogView.SCOPE_DENIED_ERROR_CODE);
        assertThat(functions.dangerousWriteInvoked).isFalse();
    }

    @Test
    void rejectsPolicyViewFromAnotherCatalogBeforeProviderRuns() {
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        AIFunctionCatalog root = new AIFunctionCatalog(mapper);
        root.registerInstances(new SameNameFunction());
        AIFunctionCatalog other = new AIFunctionCatalog(mapper);
        other.registerInstances(new SameNameFunction());
        AtomicBoolean providerCalled = new AtomicBoolean();
        AIAgentRuntime runtime = new AIAgentRuntime(
                root,
                (catalog, definition, job) -> new AIFunctionCatalogView(other, ignored -> true),
                List.of(provider("test", providerCalled, request -> completed()))
        );

        AIAgentExecutionResult result = runtime.execute(
                new AIAgentDefinition("agent", "", "Work.", "test", Set.of("same_name")),
                new AIAgentJob("job-mismatch", "work", 0, Map.of()),
                new AIAgentExecutionBudget(Duration.ofMinutes(1), 1, 0)
        );

        assertThat(providerCalled).isFalse();
        assertThat(result.output().path("errorCode").asText())
                .isEqualTo(AIAgentRuntime.FUNCTION_VIEW_MISMATCH);
    }

    @Test
    void enforcesToolBudgetBeforeSecondSideEffect() {
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        AIFunctionCatalog catalog = new AIFunctionCatalog(mapper);
        CountingFunctions functions = new CountingFunctions();
        catalog.registerInstances(functions);
        AIAgentProvider provider = provider("test", new AtomicBoolean(), request -> {
            assertThat(request.functionView().invokeResult("counted_write", mapper.createObjectNode()).isSuccess())
                    .isTrue();
            var second = request.functionView().invokeResult("counted_write", mapper.createObjectNode());
            return new AIAgentExecutionResult(AIAgentExecutionStatus.COMPLETED, second.getPayload(), 999, 0, "");
        });
        AIAgentRuntime runtime = new AIAgentRuntime(
                catalog,
                (root, definition, job) -> new AIFunctionCatalogView(root, ignored -> true),
                List.of(provider)
        );

        AIAgentExecutionResult result = runtime.execute(
                new AIAgentDefinition("writer", "", "Write once.", "test", Set.of("counted_write")),
                new AIAgentJob("job-budget", "write", 0, Map.of()),
                new AIAgentExecutionBudget(Duration.ofMinutes(1), 1, 0)
        );

        assertThat(functions.invocations.get()).isEqualTo(1);
        assertThat(result.toolCalls()).isEqualTo(1);
        assertThat(result.output().path("errorCode").asText())
                .isEqualTo(AIFunctionCatalogView.INVOCATION_BUDGET_EXCEEDED_ERROR_CODE);
    }

    @Test
    void timesOutProviderExecution() {
        AIFunctionCatalog catalog = new AIFunctionCatalog(new ObjectMapper().findAndRegisterModules());
        AIAgentProvider provider = new AIAgentProvider() {
            public String providerId() { return "blocking"; }
            public AIAgentExecutionResult execute(AIAgentExecutionRequest request) throws Exception {
                Thread.sleep(Duration.ofSeconds(5));
                return completed();
            }
        };
        AIAgentRuntime runtime = new AIAgentRuntime(
                catalog,
                (root, definition, job) -> new AIFunctionCatalogView(root, ignored -> true),
                List.of(provider)
        );

        AIAgentExecutionResult result = runtime.execute(
                new AIAgentDefinition("agent", "", "Wait.", "blocking", Set.of()),
                new AIAgentJob("job-timeout", "wait", 0, Map.of()),
                new AIAgentExecutionBudget(Duration.ofMillis(75), 0, 0)
        );

        assertThat(result.output().path("errorCode").asText()).isEqualTo(AIAgentRuntime.EXECUTION_TIMEOUT);
    }

    @Test
    void failsClosedForMissingProvider() {
        AIFunctionCatalog catalog = new AIFunctionCatalog(new ObjectMapper().findAndRegisterModules());
        AIAgentRuntime runtime = new AIAgentRuntime(
                catalog,
                (root, definition, job) -> new AIFunctionCatalogView(root, ignored -> false),
                List.of()
        );
        AIAgentExecutionResult result = runtime.execute(
                new AIAgentDefinition("agent", "", "Work.", "missing", Set.of()),
                new AIAgentJob("job-2", "work", 0, Map.of()),
                new AIAgentExecutionBudget(Duration.ofMinutes(1), 0, 0)
        );
        assertThat(result.output().path("errorCode").asText()).isEqualTo(AIAgentRuntime.PROVIDER_NOT_FOUND);
    }

    private static AIAgentProvider provider(String id, AtomicBoolean called, ProviderBody body) {
        return new AIAgentProvider() {
            public String providerId() { return id; }
            public AIAgentExecutionResult execute(AIAgentExecutionRequest request) throws Exception {
                called.set(true);
                return body.execute(request);
            }
        };
    }

    private static AIAgentExecutionResult completed() {
        return new AIAgentExecutionResult(
                AIAgentExecutionStatus.COMPLETED,
                JsonNodeFactory.instance.objectNode(),
                0,
                0,
                ""
        );
    }

    @FunctionalInterface
    private interface ProviderBody {
        AIAgentExecutionResult execute(AIAgentExecutionRequest request) throws Exception;
    }

    private static final class TestFunctions {
        private boolean dangerousWriteInvoked;
        @AIFunction(name = "safe_read", description = "Safe read") String safeRead() { return "ok"; }
        @AIFunction(name = "dangerous_write", description = "Dangerous write") String dangerousWrite() {
            dangerousWriteInvoked = true;
            return "oops";
        }
    }

    private static final class CountingFunctions {
        private final AtomicInteger invocations = new AtomicInteger();
        @AIFunction(name = "counted_write", description = "One mutation") int write() {
            return invocations.incrementAndGet();
        }
    }

    private static final class SameNameFunction {
        @AIFunction(name = "same_name", description = "Same name") String run() { return "ok"; }
    }
}

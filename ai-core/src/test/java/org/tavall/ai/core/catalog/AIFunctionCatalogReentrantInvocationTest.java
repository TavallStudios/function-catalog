package org.tavall.ai.core.catalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.tavall.ai.core.annotation.AIFunction;
import org.tavall.ai.core.invocation.AIFunctionInvocationResult;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/** Proves a catalog function may synchronously inspect a nested authorized view. */
class AIFunctionCatalogReentrantInvocationTest {
    @Test
    void functionInvocationDoesNotHoldCatalogMonitorDuringNestedViewInspection() throws Exception {
        AIFunctionCatalog catalog = new AIFunctionCatalog(new ObjectMapper());
        ReentrantFunction function = new ReentrantFunction(catalog);
        catalog.registerInstances(function);
        AIFunctionCatalogView view = new AIFunctionCatalogView(catalog, ignored -> true);

        AIFunctionInvocationResult result = java.util.concurrent.CompletableFuture
                .supplyAsync(() -> view.invokeResult("reentrant", new ObjectMapper().createObjectNode()))
                .get(5, TimeUnit.SECONDS);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getPayload().path("visibleFunctions").asInt()).isEqualTo(1);
    }

    static final class ReentrantFunction {
        private final AIFunctionCatalog catalog;

        ReentrantFunction(AIFunctionCatalog catalog) {
            this.catalog = catalog;
        }

        @AIFunction(name = "reentrant", description = "Inspect the catalog from inside a function call.")
        public Object invoke() {
            AIFunctionCatalogView nestedView = new AIFunctionCatalogView(catalog, ignored -> true);
            return java.util.Map.of("visibleFunctions", nestedView.getFunctionDefinitions().size());
        }
    }
}

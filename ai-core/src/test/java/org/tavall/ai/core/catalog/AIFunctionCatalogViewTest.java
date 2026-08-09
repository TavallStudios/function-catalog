package org.tavall.ai.core.catalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.tavall.ai.core.annotation.AIFunction;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class AIFunctionCatalogViewTest {
    @Test
    void filtersDiscoveryAndFailsClosedForHiddenInvocations() {
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        TestFunctions functions = new TestFunctions();
        AIFunctionCatalog catalog = new AIFunctionCatalog(mapper);
        catalog.registerInstances(functions);
        AIFunctionCatalogView view = new AIFunctionCatalogView(
                catalog,
                definition -> definition.getName().startsWith("agent_")
        );

        assertThat(view.getFunctionDefinitions()).containsOnlyKeys("agent_status");
        assertThat(view.getFunctionDefinitions().get("agent_status"))
                .isInstanceOf(AIFunctionPublicationDefinition.class);
        assertThat(view.allows("agent_status")).isTrue();
        assertThat(view.allows("internal_delete")).isFalse();
        assertThat(view.invokeResult("agent_status", mapper.createObjectNode()).isSuccess()).isTrue();
        var denied = view.invokeResult("internal_delete", mapper.createObjectNode());
        assertThat(denied.getErrorCode()).isEqualTo(AIFunctionCatalogView.SCOPE_DENIED_ERROR_CODE);
        assertThat(functions.hiddenInvocations.get()).isZero();
    }

    @Test
    void publicationDefinitionsContainNoInvocationObjects() {
        assertThat(Arrays.stream(AIFunctionPublicationDefinition.class.getDeclaredFields()).map(Field::getName))
                .doesNotContain("method", "target", "ownerType", "javaType");
        assertThat(Arrays.stream(AIFunctionPublicationDefinition.class.getDeclaredMethods()).map(Method::getReturnType))
                .doesNotContain(Method.class, Class.class, AIFunctionDefinition.class);
    }

    @Test
    void trustedCatalogDefinitionApiRemainsSourceCompatible() {
        assertThat(Arrays.stream(AIFunctionDefinition.class.getMethods()).map(Method::getName))
                .contains("getTarget", "getMethod");
    }

    @Test
    void enforcesInvocationBudgetBeforeSecondSideEffect() {
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        CountingFunctions functions = new CountingFunctions();
        AIFunctionCatalog catalog = new AIFunctionCatalog(mapper);
        catalog.registerInstances(functions);
        AIFunctionCatalogView view = new AIFunctionCatalogView(catalog, ignored -> true)
                .withInvocationLimit(1);

        assertThat(view.invokeResult("counted_write", mapper.createObjectNode()).isSuccess()).isTrue();
        var denied = view.invokeResult("counted_write", mapper.createObjectNode());

        assertThat(denied.getErrorCode())
                .isEqualTo(AIFunctionCatalogView.INVOCATION_BUDGET_EXCEEDED_ERROR_CODE);
        assertThat(functions.invocations.get()).isEqualTo(1);
        assertThat(view.invocationCount()).isEqualTo(1);
    }

    @Test
    void derivedViewCannotWidenParentBudgetAndSharesRevocation() {
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        CountingFunctions functions = new CountingFunctions();
        AIFunctionCatalog catalog = new AIFunctionCatalog(mapper);
        catalog.registerInstances(functions);
        AIFunctionCatalogView parent = new AIFunctionCatalogView(catalog, ignored -> true)
                .withInvocationLimit(1);
        AIFunctionCatalogView derived = parent.narrow(ignored -> true).withInvocationLimit(100);

        assertThat(derived.invokeResult("counted_write", mapper.createObjectNode()).isSuccess()).isTrue();
        assertThat(derived.invokeResult("counted_write", mapper.createObjectNode()).getErrorCode())
                .isEqualTo(AIFunctionCatalogView.INVOCATION_BUDGET_EXCEEDED_ERROR_CODE);
        assertThat(parent.invocationCount()).isEqualTo(1);

        parent.revoke();
        assertThat(derived.getFunctionDefinitions()).isEmpty();
        assertThat(derived.invokeResult("counted_write", mapper.createObjectNode()).getErrorCode())
                .isEqualTo(AIFunctionCatalogView.VIEW_REVOKED_ERROR_CODE);
        assertThat(functions.invocations.get()).isEqualTo(1);
    }

    @Test
    void narrowPreservesCatalogIdentity() {
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        AIFunctionCatalog catalog = new AIFunctionCatalog(mapper);
        catalog.registerInstances(new TestFunctions());
        AIFunctionCatalogView narrowed = new AIFunctionCatalogView(
                catalog,
                definition -> definition.getName().startsWith("agent_")
        ).narrow(definition -> definition.getName().endsWith("status"));

        assertThat(narrowed.isBackedBy(catalog)).isTrue();
        assertThat(narrowed.getFunctionDefinitions()).containsOnlyKeys("agent_status");
    }

    private static final class TestFunctions {
        private final AtomicInteger hiddenInvocations = new AtomicInteger();

        @AIFunction(name = "agent_status", description = "Visible agent capability")
        String status() { return "ok"; }

        @AIFunction(name = "internal_delete", description = "Internal-only capability")
        String delete() {
            hiddenInvocations.incrementAndGet();
            return "deleted";
        }
    }

    private static final class CountingFunctions {
        private final AtomicInteger invocations = new AtomicInteger();

        @AIFunction(name = "counted_write", description = "One observable invocation")
        int countedWrite() { return invocations.incrementAndGet(); }
    }
}

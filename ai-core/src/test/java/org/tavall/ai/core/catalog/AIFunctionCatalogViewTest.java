package org.tavall.ai.core.catalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.tavall.ai.core.annotation.AIFunction;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class AIFunctionCatalogViewTest {
    @Test
    void filtersDiscoveryAndFailsClosedForHiddenInvocations() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        TestFunctions functions = new TestFunctions();
        AIFunctionCatalog catalog = new AIFunctionCatalog(objectMapper);
        catalog.registerInstances(functions);

        AIFunctionCatalogView view = new AIFunctionCatalogView(
                catalog,
                definition -> definition.getName().startsWith("agent_")
        );

        assertThat(view.getFunctionDefinitions()).containsOnlyKeys("agent_status");
        assertThat(view.allows("agent_status")).isTrue();
        assertThat(view.allows("internal_delete")).isFalse();

        var allowed = view.invokeResult("agent_status", objectMapper.createObjectNode());
        var denied = view.invokeResult("internal_delete", objectMapper.createObjectNode());

        assertThat(allowed.isSuccess()).isTrue();
        assertThat(allowed.getPayload().asText()).isEqualTo("ok");
        assertThat(denied.isError()).isTrue();
        assertThat(denied.getErrorCode()).isEqualTo(AIFunctionCatalogView.SCOPE_DENIED_ERROR_CODE);
        assertThat(functions.hiddenInvocations.get()).isZero();
    }

    @Test
    void publishedDefinitionsDoNotExposeLiveInvocationTargets() {
        assertThat(Arrays.stream(AIFunctionDefinition.class.getMethods()).map(Method::getName))
                .doesNotContain("getTarget", "getMethod");
    }

    private static final class TestFunctions {
        private final AtomicInteger hiddenInvocations = new AtomicInteger();

        @AIFunction(name = "agent_status", description = "Visible agent capability")
        String status() {
            return "ok";
        }

        @AIFunction(name = "internal_delete", description = "Hidden destructive capability")
        String delete() {
            hiddenInvocations.incrementAndGet();
            return "deleted";
        }
    }
}

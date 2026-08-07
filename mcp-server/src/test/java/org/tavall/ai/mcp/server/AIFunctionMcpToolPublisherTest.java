package org.tavall.ai.mcp.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.tavall.ai.core.annotation.AIFunction;
import org.tavall.ai.core.catalog.AIFunctionCatalog;

import static org.assertj.core.api.Assertions.assertThat;

class AIFunctionMcpToolPublisherTest {
    @Test
    void publishesOnlyDefinitionsAcceptedByCapabilityFilter() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        AIFunctionCatalog catalog = new AIFunctionCatalog(objectMapper);
        catalog.registerInstances(new TestFunctions());

        var specifications = new AIFunctionMcpToolPublisher(objectMapper).toolSpecifications(
                catalog,
                definition -> definition.getName().startsWith("chatgpt_")
        );

        assertThat(catalog.getFunctionDefinitions()).hasSize(2);
        assertThat(specifications).hasSize(1);
    }

    private static final class TestFunctions {
        @AIFunction(name = "chatgpt_status", description = "Visible ChatGPT capability")
        String chatGPTStatus() {
            return "ok";
        }

        @AIFunction(name = "internal_reconcile", description = "Internal-only capability")
        String internalReconcile() {
            return "ok";
        }
    }
}

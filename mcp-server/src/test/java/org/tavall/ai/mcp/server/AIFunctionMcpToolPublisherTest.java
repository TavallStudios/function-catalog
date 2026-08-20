package org.tavall.ai.mcp.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;
import org.tavall.ai.core.annotation.AIFunction;
import org.tavall.ai.core.catalog.AIFunctionCatalog;
import org.tavall.ai.core.invocation.AIFunctionImageContent;
import org.tavall.ai.core.invocation.AIFunctionOutput;
import org.tavall.ai.core.invocation.AIFunctionResourceContent;

import java.util.Map;

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

        assertThat(catalog.getFunctionDefinitions()).hasSize(3);
        assertThat(specifications).hasSize(2);
    }

    @Test
    void publishesCanonicalOutputSchemaForTypedResults() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        AIFunctionCatalog catalog = new AIFunctionCatalog(objectMapper);
        catalog.registerInstances(new TestFunctions());

        var tool = new AIFunctionMcpToolPublisher(objectMapper).toolSpecifications(catalog).stream()
                .map(specification -> specification.tool())
                .filter(candidate -> candidate.name().equals("chatgpt_status"))
                .findFirst()
                .orElseThrow();

        assertThat(tool.outputSchema()).isNotNull();
        assertThat(tool.outputSchema()).containsEntry("type", "object");
        Map<?, ?> properties = (Map<?, ?>) tool.outputSchema().get("properties");
        assertThat(properties).containsKeys("status", "sequence");
    }

    @Test
    void doesNotPublishFalseStaticOutputSchemaForDynamicRichOutput() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        AIFunctionCatalog catalog = new AIFunctionCatalog(objectMapper);
        catalog.registerInstances(new TestFunctions());

        var tool = new AIFunctionMcpToolPublisher(objectMapper).toolSpecifications(catalog).stream()
                .map(specification -> specification.tool())
                .filter(candidate -> candidate.name().equals("chatgpt_rich"))
                .findFirst()
                .orElseThrow();

        assertThat(tool.outputSchema()).isNull();
    }

    @Test
    void projectsRichFunctionOutputIntoNativeMcpImageAndResourceContent() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        AIFunctionCatalog catalog = new AIFunctionCatalog(objectMapper);
        catalog.registerInstances(new TestFunctions());

        var specification = new AIFunctionMcpToolPublisher(objectMapper).toolSpecifications(catalog).stream()
                .filter(candidate -> candidate.tool().name().equals("chatgpt_rich"))
                .findFirst()
                .orElseThrow();
        McpSchema.CallToolResult result = specification.callHandler().apply(
                null,
                new McpSchema.CallToolRequest("chatgpt_rich", Map.of())
        );

        assertThat(result.isError()).isFalse();
        assertThat(result.structuredContent()).isEqualTo(Map.of("status", "ok"));
        assertThat(result.content()).hasSize(3);
        assertThat(result.content().get(0)).isInstanceOfSatisfying(
                McpSchema.TextContent.class,
                text -> assertThat(text.text()).isEqualTo("{\"status\":\"ok\"}")
        );
        assertThat(result.content().get(1)).isInstanceOfSatisfying(
                McpSchema.ImageContent.class,
                image -> {
                    assertThat(image.mimeType()).isEqualTo("image/png");
                    assertThat(image.data()).isEqualTo("AQID");
                }
        );
        assertThat(result.content().get(2)).isInstanceOfSatisfying(
                McpSchema.EmbeddedResource.class,
                embedded -> {
                    assertThat(embedded.resource()).isInstanceOf(McpSchema.BlobResourceContents.class);
                    McpSchema.BlobResourceContents resource = (McpSchema.BlobResourceContents) embedded.resource();
                    assertThat(resource.uri()).isEqualTo("tavall-artifact://test/sample.bin");
                    assertThat(resource.mimeType()).isEqualTo("application/octet-stream");
                    assertThat(resource.blob()).isEqualTo("BAU=");
                }
        );
    }

    private static final class TestFunctions {
        @AIFunction(name = "chatgpt_status", description = "Visible ChatGPT capability")
        StatusResult chatGPTStatus() {
            return new StatusResult("ok", 7);
        }

        @AIFunction(name = "chatgpt_rich", description = "Rich ChatGPT capability")
        AIFunctionOutput richOutput() {
            return AIFunctionOutput.of(
                    Map.of("status", "ok"),
                    new AIFunctionImageContent(new byte[]{1, 2, 3}, "image/png"),
                    new AIFunctionResourceContent(
                            new byte[]{4, 5},
                            "application/octet-stream",
                            "tavall-artifact://test/sample.bin"
                    )
            );
        }

        @AIFunction(name = "internal_reconcile", description = "Internal-only capability")
        String internalReconcile() {
            return "ok";
        }
    }

    private record StatusResult(String status, int sequence) {
    }
}

package org.tavall.anthropic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.jupiter.api.Test;
import org.tavall.ai.core.catalog.AIFunctionCatalog;
import org.tavall.anthropic.clients.AnthropicClientWrapper;
import org.tavall.anthropic.clients.adapter.AnthropicToolAdapter;
import org.tavall.anthropic.clients.parser.AnthropicToolCallParser;
import org.tavall.anthropic.clients.response.AnthropicToolResultBlock;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnthropicClientWrapperTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void exportsFunctionsParsesBlocksAndExecutesThroughCatalog() throws Exception {
        AIFunctionCatalog catalog = new AIFunctionCatalog(objectMapper);
        catalog.registerInstances(List.of(new AnthropicMathService()));

        AnthropicClientWrapper wrapper = new AnthropicClientWrapper(
                objectMapper,
                catalog,
                new AnthropicToolAdapter(objectMapper),
                new AnthropicToolCallParser(objectMapper)
        );

        ArrayNode toolPayload = wrapper.buildToolPayload();
        assertEquals(1, toolPayload.size());
        assertEquals("AnthropicMathService_multiply", toolPayload.get(0).path("name").asText());
        assertTrue(toolPayload.get(0).path("input_schema").has("properties"));

        JsonNode response = objectMapper.readTree(
                """
                {
                  "content": [
                    {
                      "type": "text",
                      "text": "Running tool"
                    },
                    {
                      "type": "tool_use",
                      "id": "toolu_1",
                      "name": "AnthropicMathService_multiply",
                      "input": {
                        "left": 4,
                        "right": 5
                      }
                    }
                  ]
                }
                """
        );

        List<AnthropicToolResultBlock> results = wrapper.executeToolCalls(response);
        assertEquals(1, results.size());
        assertEquals("toolu_1", results.get(0).getToolUseId());
        assertEquals("AnthropicMathService_multiply", results.get(0).getFunctionName());
        assertFalse(results.get(0).isError());
        assertEquals(20, results.get(0).getResult().path("product").asInt());

        ArrayNode toolResultContent = wrapper.toToolResultContent(results);
        assertEquals(1, toolResultContent.size());
        assertEquals("tool_result", toolResultContent.get(0).path("type").asText());
        assertEquals("toolu_1", toolResultContent.get(0).path("tool_use_id").asText());
    }

    @Test
    void marksAnthropicToolErrorsWithoutThrowingWholeBatch() throws Exception {
        AIFunctionCatalog catalog = new AIFunctionCatalog(objectMapper);
        catalog.registerInstances(List.of(new AnthropicMathService()));

        AnthropicClientWrapper wrapper = new AnthropicClientWrapper(
                objectMapper,
                catalog,
                new AnthropicToolAdapter(objectMapper),
                new AnthropicToolCallParser(objectMapper)
        );

        JsonNode response = objectMapper.readTree(
                """
                {
                  "content": [
                    {
                      "type": "tool_use",
                      "id": "toolu_2",
                      "name": "AnthropicMathService_multiply",
                      "input": {
                        "left": 4
                      }
                    }
                  ]
                }
                """
        );

        List<AnthropicToolResultBlock> results = wrapper.executeToolCalls(response);
        assertEquals(1, results.size());
        assertTrue(results.get(0).isError());
        assertEquals("invalid_arguments", results.get(0).getErrorCode());

        ArrayNode toolResultContent = wrapper.toToolResultContent(results);
        assertTrue(toolResultContent.get(0).path("is_error").asBoolean());
    }
}

package org.tavall.openai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.jupiter.api.Test;
import org.tavall.ai.core.catalog.AIFunctionCatalog;
import org.tavall.openai.clients.OpenAIClientWrapper;
import org.tavall.openai.clients.adapter.OpenAIFunctionAdapter;
import org.tavall.openai.clients.parser.OpenAIFunctionCallParser;
import org.tavall.openai.clients.response.OpenAIFunctionResultMessage;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenAIClientWrapperTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void exportsFunctionsParsesCallsAndExecutesThroughCatalog() throws Exception {
        AIFunctionCatalog catalog = new AIFunctionCatalog(objectMapper);
        catalog.registerInstances(List.of(new OpenAIMathService()));

        OpenAIClientWrapper wrapper = new OpenAIClientWrapper(
                objectMapper,
                catalog,
                new OpenAIFunctionAdapter(objectMapper),
                new OpenAIFunctionCallParser(objectMapper)
        );

        ArrayNode toolPayload = wrapper.buildToolPayload();
        assertEquals(1, toolPayload.size());
        assertEquals("function", toolPayload.get(0).path("type").asText());
        assertEquals("OpenAIMathService_add", toolPayload.get(0).path("function").path("name").asText());
        assertTrue(toolPayload.get(0).path("function").path("parameters").has("properties"));

        JsonNode response = objectMapper.readTree(
                """
                {
                  "choices": [
                    {
                      "message": {
                        "tool_calls": [
                          {
                            "id": "call_1",
                            "type": "function",
                            "function": {
                              "name": "OpenAIMathService_add",
                              "arguments": "{\\"left\\":2,\\"right\\":3}"
                            }
                          }
                        ]
                      }
                    }
                  ]
                }
                """
        );

        List<OpenAIFunctionResultMessage> results = wrapper.executeToolCalls(response);
        assertEquals(1, results.size());
        assertEquals("call_1", results.get(0).getToolCallId());
        assertEquals("OpenAIMathService_add", results.get(0).getFunctionName());
        assertFalse(results.get(0).isError());
        assertEquals(5, results.get(0).getResult().path("sum").asInt());

        ArrayNode toolMessages = wrapper.toToolMessagePayload(results);
        assertEquals(1, toolMessages.size());
        assertEquals("tool", toolMessages.get(0).path("role").asText());
        assertEquals("call_1", toolMessages.get(0).path("tool_call_id").asText());
    }

    @Test
    void serializesInvocationFailuresWithoutThrowingWholeBatch() throws Exception {
        AIFunctionCatalog catalog = new AIFunctionCatalog(objectMapper);
        catalog.registerInstances(List.of(new OpenAIMathService()));

        OpenAIClientWrapper wrapper = new OpenAIClientWrapper(
                objectMapper,
                catalog,
                new OpenAIFunctionAdapter(objectMapper),
                new OpenAIFunctionCallParser(objectMapper)
        );

        JsonNode response = objectMapper.readTree(
                """
                {
                  "choices": [
                    {
                      "message": {
                        "tool_calls": [
                          {
                            "id": "call_2",
                            "type": "function",
                            "function": {
                              "name": "OpenAIMathService_add",
                              "arguments": "{\\"left\\":2}"
                            }
                          }
                        ]
                      }
                    }
                  ]
                }
                """
        );

        List<OpenAIFunctionResultMessage> results = wrapper.executeToolCalls(response);
        assertEquals(1, results.size());
        assertTrue(results.get(0).isError());
        assertEquals("invalid_arguments", results.get(0).getErrorCode());
        assertEquals(false, results.get(0).getResult().path("success").asBoolean(true));
    }
}

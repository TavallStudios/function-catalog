# openai-sdk

`openai-sdk` adapts canonical tools from `ai-core` into OpenAI function/tool payloads and executes returned tool calls.

## Build catalog and wrapper

```java
ObjectMapper objectMapper = new ObjectMapper();
AIFunctionCatalog catalog = new AIFunctionCatalog(objectMapper);
catalog.registerTargets(List.of(new WeatherTools()));

OpenAIClientWrapper wrapper = new OpenAIClientWrapper(
        objectMapper,
        catalog,
        new OpenAIFunctionAdapter(objectMapper),
        new OpenAIFunctionCallParser(objectMapper)
);
```

## Export OpenAI tool definitions

```java
ArrayNode tools = wrapper.buildToolPayload();
```

## Parse OpenAI response and invoke tools

```java
List<OpenAIFunctionResultMessage> results = wrapper.executeToolCalls(openAiResponseJson);
ArrayNode toolMessages = wrapper.toToolMessagePayload(results);
```

`toolMessages` can be appended to the next OpenAI request as tool outputs.
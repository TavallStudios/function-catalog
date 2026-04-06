# claude-sdk

`claude-sdk` adapts canonical tools from `ai-core` into Anthropic `tools` entries and executes `tool_use` blocks.

## Build catalog and wrapper

```java
ObjectMapper objectMapper = new ObjectMapper();
AIFunctionCatalog catalog = new AIFunctionCatalog(objectMapper);
catalog.registerTargets(List.of(new WeatherTools()));

AnthropicClientWrapper wrapper = new AnthropicClientWrapper(
        objectMapper,
        catalog,
        new AnthropicToolAdapter(objectMapper),
        new AnthropicToolCallParser(objectMapper)
);
```

## Export Anthropic tool definitions

```java
ArrayNode tools = wrapper.buildToolPayload();
```

## Parse `tool_use` blocks and invoke tools

```java
List<AnthropicToolResultBlock> results = wrapper.executeToolCalls(anthropicResponseJson);
ArrayNode toolResultBlocks = wrapper.toToolResultContent(results);
```

`toolResultBlocks` can be sent back in the next Anthropic message payload.
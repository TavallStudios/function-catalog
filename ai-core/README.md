# ai-core

`ai-core` provides canonical tool definitions, JSON Schema generation, and reflective invocation routing.

## Mark methods as tools

```java
import org.tavall.ai.core.annotation.AIFunction;
import org.tavall.ai.core.annotation.AIParam;

public final class WeatherTools {
    @AIFunction(description = "Get weather")
    public String getWeather(
            @AIParam(name = "city") String city,
            @AIParam(name = "unit", required = false) String unit
    ) {
        return "ok";
    }
}
```

## Build catalog at startup

```java
ObjectMapper objectMapper = new ObjectMapper();
AIFunctionCatalog catalog = new AIFunctionCatalog(objectMapper);
AIFunctionBootstrapper bootstrapper = new AIFunctionBootstrapper(catalog);
bootstrapper.bootstrap(List.of(new WeatherTools()));
```

## Export canonical schemas

```java
ArrayNode schemas = catalog.exportCanonicalToolSchemas();
```

## Route provider-agnostic tool calls

```java
AIFunctionInvocationRouter router = new AIFunctionInvocationRouter(catalog, objectMapper);
AIFunctionCall toolCall = new AIFunctionCall("WeatherTools_getWeather", argsJson);
JsonNode result = router.invokeAsJson(toolCall);
```

## Minecraft WorldOps

`org.tavall.ai.minecraft.worldops` provides the bounded typed Function Catalog surface for Minecraft world mutation.

Composition is deliberately one-way:

```text
MinecraftWorldOpsFunctions
    -> MinecraftWorldOpsService
        -> MinecraftWorldOpsProvider
```

Register a host-scoped provider through the normal catalog registrar path:

```java
AIFunctionCatalog catalog = new AIFunctionCatalog(objectMapper);
catalog.registerRegistrars(List.of(new MinecraftWorldOpsRegistrar(provider)));
```

The provider is the only external-runtime boundary. `ai-core` does not depend on Mineflayer, FAWE, Paper, RCON, shell execution, or host credentials. WorldOps exposes typed world, coordinate, region, block-state, clipboard, schematic, and history operations and intentionally provides no generic command function. MCP and agent consumers receive the same canonical names and generated schemas through the existing Function Catalog machinery.
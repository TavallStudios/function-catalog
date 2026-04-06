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
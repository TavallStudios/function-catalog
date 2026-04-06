package org.tavall.ai.core.catalog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.tavall.ai.core.invocation.AIFunctionInvocationResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AIFunctionCatalogTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void invokesRegisteredFunctionWithEnumListAndNestedRecordPojoArguments() {
        AIFunctionCatalog catalog = new AIFunctionCatalog(objectMapper);
        catalog.registerInstances(List.of(new WeatherToolService()));

        JsonNode schema = catalog.exportCanonicalFunctionSchemas().get(0);
        assertEquals("WeatherToolService_summarize", schema.path("name").asText());
        assertEquals("array", schema.path("parameters").path("properties").path("days").path("type").asText());
        assertEquals("string", schema.path("parameters").path("properties").path("unit").path("type").asText());
        assertEquals(
                "string",
                schema.path("parameters")
                        .path("properties")
                        .path("context")
                        .path("properties")
                        .path("location")
                        .path("properties")
                        .path("countryCode")
                        .path("type")
                        .asText()
        );

        ObjectNode arguments = objectMapper.createObjectNode();
        arguments.put("city", "Austin");
        arguments.put("unit", "CELSIUS");
        arguments.putArray("days").add(1).add(2);
        ObjectNode context = arguments.putObject("context");
        ObjectNode location = context.putObject("location");
        location.put("countryCode", "US");
        location.put("zipCode", "73301");
        context.putArray("tags").add("warm").add("dry");
        context.putObject("thresholdByDay").put("1", 75).put("2", 76);

        Object result = catalog.invoke("WeatherToolService_summarize", arguments);
        WeatherSummary summary = assertInstanceOf(WeatherSummary.class, result);
        assertEquals("Austin:CELSIUS", summary.summary());
        assertEquals(2, summary.daysRequested());
        assertEquals("US", summary.countryCode());
        assertEquals("warm|dry", summary.notes());
    }

    @Test
    void manualRegistrationSupportsPrivateMethodsInterfaceAnnotationsAndOptionalEmpty() {
        AIFunctionCatalog catalog = new AIFunctionCatalog(objectMapper);
        catalog.registerInstances(List.of(
                new PrivateFunctionService(),
                new InterfaceAnnotatedFunctionImpl(),
                new OptionalFunctionService()));

        ObjectNode joinArguments = objectMapper.createObjectNode();
        joinArguments.put("left", "ab");
        joinArguments.put("right", "cd");
        assertEquals("abcd", catalog.invoke("private_join", joinArguments));

        ObjectNode echoArguments = objectMapper.createObjectNode();
        echoArguments.put("message", "hello");
        assertEquals("echo:hello", catalog.invoke("interface_echo", echoArguments));

        JsonNode optionalSchema = catalog.getFunctionDefinitions().get("optional_prefix").getCanonicalParametersSchema();
        assertFalse(optionalSchema.path("required").toString().contains("prefix"));
        assertEquals("EMPTY", catalog.invoke("optional_prefix", objectMapper.createObjectNode()));
    }

    @Test
    void failsWhenDuplicateFunctionNamesAreRegisteredManually() {
        AIFunctionCatalog catalog = new AIFunctionCatalog(objectMapper);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> catalog.registerInstances(List.of(new DuplicateNameServiceA(), new DuplicateNameServiceB()))
        );

        assertTrue(exception.getMessage().contains("Duplicate function name 'shared_tool'"));
    }

    @Test
    void appliesAIParamOverridesAndManualRegistrationWinsOverScannedDuplicates() {
        AIFunctionCatalog catalog = new AIFunctionCatalog(objectMapper);
        catalog.registerInstances(List.of(new ParamOverrideService(), new ManualWinningFunctionService()));
        catalog.scanPackages(List.of("org.tavall.ai.core.scanfixtures"));

        JsonNode schema = catalog.exportCanonicalFunctionSchemas().get(0);
        JsonNode properties = schema.path("parameters").path("properties");
        assertNotNull(properties.get("optional_note"));
        assertNotNull(properties.get("user_id"));
        assertEquals("User identifier", properties.path("user_id").path("description").asText());

        ObjectNode arguments = objectMapper.createObjectNode();
        arguments.put("user_id", "123");
        OverrideResult overrideResult = assertInstanceOf(OverrideResult.class, catalog.invoke("custom_tool", arguments));
        assertEquals("123", overrideResult.user());
        assertEquals("", overrideResult.note());

        ObjectNode scannedArguments = objectMapper.createObjectNode();
        scannedArguments.put("name", "Tav");
        assertEquals("scan:Tav", catalog.invoke("scan_only", scannedArguments));
        assertEquals("manual", catalog.invoke("shared_function", objectMapper.createObjectNode()));
    }

    @Test
    void persistsStateWritesSnapshotsAndHotReloadsDisabledAndSettingsUpdates() throws Exception {
        Path tempDirectory = Files.createTempDirectory("function-catalog-state");
        Path stateFile = tempDirectory.resolve("function-catalog-state.json");
        Path snapshotFile = tempDirectory.resolve("function-catalog-snapshot.json");

        AIFunctionCatalog catalog = new AIFunctionCatalog(objectMapper)
                .configureStateFiles(stateFile, snapshotFile);
        catalog.registerInstances(List.of(new ConfigurableGreetingService()));

        AIFunctionDefinition definition = catalog.getFunctionDefinitions().get("configured_greet");
        assertNotNull(definition);
        assertTrue(definition.isEnabled());
        assertTrue(definition.hasSettings());
        assertTrue(Files.exists(stateFile));
        assertTrue(Files.exists(snapshotFile));

        JsonNode initialSnapshot = objectMapper.readTree(Files.readString(snapshotFile));
        assertEquals(1, initialSnapshot.path("summary").path("totalFunctions").asInt());
        assertEquals(1, initialSnapshot.path("summary").path("enabledFunctions").asInt());

        writeState(stateFile, false, "Hola", true);

        ObjectNode greetArguments = objectMapper.createObjectNode();
        greetArguments.put("name", "TJ");
        AIFunctionInvocationResult disabledResult = catalog.invokeResult("configured_greet", greetArguments);
        assertFalse(disabledResult.isSuccess());
        assertEquals("disabled", disabledResult.getErrorCode());

        JsonNode disabledSnapshot = objectMapper.readTree(Files.readString(snapshotFile));
        assertEquals(0, disabledSnapshot.path("summary").path("enabledFunctions").asInt());
        assertEquals(1, disabledSnapshot.path("summary").path("disabledFunctions").asInt());
        assertEquals("Hola", disabledSnapshot.path("functions").get(0).path("settings").path("prefix").asText());

        writeState(stateFile, true, "Hola", true);
        AIFunctionInvocationResult enabledResult = catalog.invokeResult("configured_greet", greetArguments);
        assertTrue(enabledResult.isSuccess());
        assertEquals("Hola, TJ!", enabledResult.getPayload().path("message").asText());
    }

    private void writeState(Path stateFile, boolean enabled, String prefix, boolean excited) throws Exception {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("updatedAt", Instant.now().toString());
        ObjectNode functionsNode = root.putObject("functions");
        ObjectNode functionNode = functionsNode.putObject("configured_greet");
        functionNode.put("enabled", enabled);
        functionNode.put("updatedAt", Instant.now().toString());
        ObjectNode settings = functionNode.putObject("settings");
        settings.put("prefix", prefix);
        settings.put("excited", excited);
        Thread.sleep(25L);
        Files.writeString(stateFile, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root));
        Thread.sleep(25L);
    }
}

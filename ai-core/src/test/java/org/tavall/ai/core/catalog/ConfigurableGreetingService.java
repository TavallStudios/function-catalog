package org.tavall.ai.core.catalog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.tavall.ai.core.annotation.AIParam;
import org.tavall.ai.core.annotation.AIFunction;

import java.util.List;

public final class ConfigurableGreetingService implements AIFunctionSettingsContributor {
    private GreetingSettings settings = new GreetingSettings("Hello", false);

    @AIFunction(name = "configured_greet", description = "Greets with editable function settings")
    public GreetingResult greet(@AIParam(name = "name") String name) {
        String prefix = settings.prefix() == null ? "Hello" : settings.prefix();
        String suffix = settings.excited() ? "!" : "";
        return new GreetingResult(prefix + ", " + name + suffix);
    }

    @Override
    public Iterable<AIFunctionSettingsDescriptor> describeFunctionSettings() {
        return List.of(AIFunctionSettingsDescriptor.of(
                "configured_greet",
                "Greeting behavior",
                GreetingSettings.class,
                settings,
                new GreetingSettings("Hello", false)
        ));
    }

    @Override
    public void applyFunctionSettings(String functionName, JsonNode settingsNode, ObjectMapper objectMapper) {
        if (!"configured_greet".equals(functionName)) {
            throw new IllegalArgumentException("Unknown function settings target: " + functionName);
        }
        this.settings = objectMapper.convertValue(settingsNode, GreetingSettings.class);
    }
}

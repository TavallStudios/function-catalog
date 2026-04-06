package org.tavall.ai.core.catalog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public interface AIFunctionSettingsContributor {
    Iterable<AIFunctionSettingsDescriptor> describeFunctionSettings();

    void applyFunctionSettings(String functionName, JsonNode settings, ObjectMapper objectMapper);
}

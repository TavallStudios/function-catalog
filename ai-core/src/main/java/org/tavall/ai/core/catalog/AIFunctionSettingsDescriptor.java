package org.tavall.ai.core.catalog;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;

public final class AIFunctionSettingsDescriptor {
    private final String functionName;
    private final String description;
    private final JavaType settingsType;
    private final Object currentSettings;
    private final Object defaultSettings;

    public AIFunctionSettingsDescriptor(
            String functionName,
            String description,
            JavaType settingsType,
            Object currentSettings,
            Object defaultSettings
    ) {
        this.functionName = requireText(functionName, "functionName");
        this.description = description == null ? "" : description;
        this.settingsType = requireValue(settingsType, "settingsType");
        this.currentSettings = currentSettings;
        this.defaultSettings = defaultSettings;
    }

    public static AIFunctionSettingsDescriptor of(
            String functionName,
            Class<?> settingsType,
            Object currentSettings,
            Object defaultSettings
    ) {
        return new AIFunctionSettingsDescriptor(
                functionName,
                "",
                TypeFactory.defaultInstance().constructType(requireValue(settingsType, "settingsType")),
                currentSettings,
                defaultSettings
        );
    }

    public static AIFunctionSettingsDescriptor of(
            String functionName,
            String description,
            Class<?> settingsType,
            Object currentSettings,
            Object defaultSettings
    ) {
        return new AIFunctionSettingsDescriptor(
                functionName,
                description,
                TypeFactory.defaultInstance().constructType(requireValue(settingsType, "settingsType")),
                currentSettings,
                defaultSettings
        );
    }

    public String getFunctionName() {
        return functionName;
    }

    public String getDescription() {
        return description;
    }

    public JavaType getSettingsType() {
        return settingsType;
    }

    public Object getCurrentSettings() {
        return currentSettings;
    }

    public Object getDefaultSettings() {
        return defaultSettings;
    }

    private static <T> T requireValue(T value, String fieldName) {
        if (value != null) {
            return value;
        }

        throw new IllegalArgumentException(fieldName + " must not be null");
    }

    private static String requireText(String value, String fieldName) {
        if (value != null && !value.isBlank()) {
            return value;
        }

        throw new IllegalArgumentException(fieldName + " must not be blank");
    }
}

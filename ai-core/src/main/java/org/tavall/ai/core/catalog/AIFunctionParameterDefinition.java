package org.tavall.ai.core.catalog;

import com.fasterxml.jackson.databind.JavaType;

public final class AIFunctionParameterDefinition {
    private final int index;
    private final String name;
    private final String description;
    private final JavaType javaType;
    private final boolean required;

    public AIFunctionParameterDefinition(int index, String name, String description, JavaType javaType, boolean required) {
        this.index = index;
        this.name = requireText(name, "name");
        this.description = description == null ? "" : description;
        this.javaType = requireValue(javaType, "javaType");
        this.required = required;
    }

    public int getIndex() {
        return index;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public JavaType getJavaType() {
        return javaType;
    }

    public boolean isRequired() {
        return required;
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

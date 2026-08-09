package org.tavall.ai.core.catalog;

/** Publication-safe parameter metadata with no reflective Java type handle. */
public record AIFunctionPublicationParameterDefinition(
        int index,
        String name,
        String description,
        String typeName,
        boolean required
) {
    public AIFunctionPublicationParameterDefinition {
        if (index < 0) {
            throw new IllegalArgumentException("index must not be negative");
        }
        name = requireText(name, "name");
        description = description == null ? "" : description;
        typeName = requireText(typeName, "typeName");
    }

    private static String requireText(String value, String fieldName) {
        if (value != null && !value.isBlank()) {
            return value;
        }
        throw new IllegalArgumentException(fieldName + " must not be blank");
    }
}

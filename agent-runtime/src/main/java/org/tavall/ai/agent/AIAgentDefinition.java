package org.tavall.ai.agent;

import java.util.LinkedHashSet;
import java.util.Set;

/** Provider-neutral definition of a Tavall AI agent role. */
public record AIAgentDefinition(
        String id,
        String description,
        String instructions,
        String providerId,
        Set<String> requestedFunctionNames
) {
    public AIAgentDefinition {
        id = requireText(id, "id");
        description = description == null ? "" : description.trim();
        instructions = requireText(instructions, "instructions");
        providerId = requireText(providerId, "providerId");
        requestedFunctionNames = Set.copyOf(new LinkedHashSet<>(
                requestedFunctionNames == null ? Set.of() : requestedFunctionNames
        ));
        requestedFunctionNames.forEach(name -> requireText(name, "requestedFunctionNames entry"));
    }

    private static String requireText(String value, String fieldName) {
        if (value != null && !value.isBlank()) return value;
        throw new IllegalArgumentException(fieldName + " must not be blank");
    }
}

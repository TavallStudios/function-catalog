package org.tavall.ai.agent.strands;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Serializable reference telling the standalone Strands runtime to expose one
 * already-live Strands session as a native agent-as-tool capability to another session.
 */
public record StrandsAgentToolReference(
        String agentId,
        String name,
        String description
) {
    public StrandsAgentToolReference {
        agentId = requireText(agentId, "agentId");
        name = requireText(name, "name");
        description = description == null ? "" : description.trim();
    }

    public StrandsAgentToolReference(String agentId, String name) {
        this(agentId, name, "");
    }

    Map<String, Object> toMcpValue() {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("agentId", agentId);
        value.put("name", name);
        if (!description.isBlank()) {
            value.put("description", description);
        }
        return Map.copyOf(value);
    }

    private static String requireText(String value, String fieldName) {
        if (value != null && !value.isBlank()) {
            return value.trim();
        }
        throw new IllegalArgumentException(fieldName + " must not be blank");
    }
}

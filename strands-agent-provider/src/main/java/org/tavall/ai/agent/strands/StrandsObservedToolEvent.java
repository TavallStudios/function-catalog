package org.tavall.ai.agent.strands;

import java.util.Map;

public record StrandsObservedToolEvent(
        String agentId,
        String toolName,
        Map<String, Object> input,
        String status,
        String error
) {
    public StrandsObservedToolEvent {
        input = input == null ? Map.of() : Map.copyOf(input);
    }

    public boolean succeeded() {
        return error == null && !"error".equals(status);
    }
}

package org.tavall.ai.agent.strands;

import java.util.Map;

public record StrandsInvocationLimits(
        int turns,
        int outputTokens,
        int totalTokens
) {
    public StrandsInvocationLimits {
        if (turns <= 0 || outputTokens <= 0 || totalTokens <= 0) {
            throw new IllegalArgumentException("Strands invocation limits must be positive");
        }
    }

    Map<String, Object> toMcpValue() {
        return Map.of(
                "turns", turns,
                "outputTokens", outputTokens,
                "totalTokens", totalTokens
        );
    }
}

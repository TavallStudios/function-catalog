package org.tavall.ai.agent.strands;

import java.util.List;

public record StrandsObservedInvocationResult(
        String agentId,
        String text,
        String stopReason,
        List<StrandsObservedToolEvent> toolEvents
) {
    public StrandsObservedInvocationResult {
        toolEvents = List.copyOf(toolEvents);
    }
}

package org.tavall.ai.agent;

import java.time.Duration;
import java.util.Objects;

/** Runtime-level budget. Infrastructure CPU/RAM/concurrency limits remain Tavall Cloud policy. */
public record AIAgentExecutionBudget(Duration timeout, int maxToolCalls, int maxDelegations) {
    public AIAgentExecutionBudget {
        timeout = Objects.requireNonNull(timeout, "timeout");
        if (timeout.isZero() || timeout.isNegative()) throw new IllegalArgumentException("timeout must be positive");
        if (maxToolCalls < 0) throw new IllegalArgumentException("maxToolCalls must be >= 0");
        if (maxDelegations < 0) throw new IllegalArgumentException("maxDelegations must be >= 0");
    }
}

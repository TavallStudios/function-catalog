package org.tavall.ai.core.policy;

public interface AIFunctionPolicy {
    void checkInvocation(AIFunctionInvocationContext invocationContext);
}
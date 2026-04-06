package org.tavall.ai.core.policy;

public final class AllowAllAIFunctionPolicy implements AIFunctionPolicy {
    @Override
    public void checkInvocation(AIFunctionInvocationContext invocationContext) {
        // allow all invocations
    }
}

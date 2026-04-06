package org.tavall.ai.core.audit;

public final class NoOpAIFunctionAuditLogger implements AIFunctionAuditLogger {
    @Override
    public void onInvocationStart(String functionName, int argumentSizeBytes) {
        // no-op
    }

    @Override
    public void onInvocationSuccess(String functionName, int argumentSizeBytes) {
        // no-op
    }

    @Override
    public void onInvocationFailure(String functionName, int argumentSizeBytes, Throwable throwable) {
        // no-op
    }
}

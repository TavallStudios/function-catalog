package org.tavall.ai.core.audit;

public interface AIFunctionAuditLogger {
    void onInvocationStart(String functionName, int argumentSizeBytes);

    void onInvocationSuccess(String functionName, int argumentSizeBytes);

    void onInvocationFailure(String functionName, int argumentSizeBytes, Throwable throwable);
}

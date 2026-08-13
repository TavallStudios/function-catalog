package org.tavall.ai.core.audit;

/**
 * Observes the lifecycle of catalog function invocations without participating in authorization or
 * result transformation.
 *
 * <p>The catalog reports start after a function has been resolved and enabled, then reports exactly
 * one success or failure outcome for the attempted invocation path. The argument size is the
 * catalog's serialized-size estimate for the normalized JSON arguments and is intended for audit,
 * quota, and telemetry use rather than application semantics.</p>
 */
public interface AIFunctionAuditLogger {

    /**
     * Records that an enabled function is about to pass through policy and invocation processing.
     *
     * @param functionName resolved catalog function name
     * @param argumentSizeBytes estimated normalized argument size in bytes
     */
    void onInvocationStart(String functionName, int argumentSizeBytes);

    /**
     * Records that policy, argument mapping, and target invocation completed successfully.
     *
     * @param functionName resolved catalog function name
     * @param argumentSizeBytes estimated normalized argument size in bytes
     */
    void onInvocationSuccess(String functionName, int argumentSizeBytes);

    /**
     * Records the failure that prevented a started invocation from completing successfully.
     *
     * @param functionName resolved catalog function name
     * @param argumentSizeBytes estimated normalized argument size in bytes
     * @param throwable policy, argument-mapping, reflection, or target failure observed by the
     *                  catalog
     */
    void onInvocationFailure(String functionName, int argumentSizeBytes, Throwable throwable);
}

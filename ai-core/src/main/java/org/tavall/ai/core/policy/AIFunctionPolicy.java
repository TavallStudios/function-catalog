package org.tavall.ai.core.policy;

/**
 * Authorization and policy gate evaluated immediately before a catalog function is invoked.
 *
 * <p>The catalog calls this contract after resolving an enabled function and before mapping
 * arguments or invoking the target method. Policies allow execution by returning normally and deny
 * execution by throwing an exception. Policy failures are converted into normal failed invocation
 * results by the catalog and are also reported through the configured audit logger.</p>
 */
public interface AIFunctionPolicy {

    /**
     * Validates whether one resolved function invocation may proceed.
     *
     * @param invocationContext immutable invocation context containing the function name, raw JSON
     *                          arguments, and resolved function definition
     * @throws Exception when invocation must be denied or cannot be evaluated; runtime exceptions
     *                   are also accepted by the catalog's invocation boundary
     */
    void checkInvocation(AIFunctionInvocationContext invocationContext);
}

package org.tavall.ai.agent;

/**
 * Adapter between the Tavall agent runtime and one concrete AI/model execution backend.
 *
 * <p>Provider selection is performed by the provider identifier. Once selected, the runtime passes
 * a fully resolved execution request to {@link #execute(AIAgentExecutionRequest)} and expects the
 * provider to return Tavall's provider-neutral execution result rather than leaking backend SDK
 * response types into the runtime.</p>
 */
public interface AIAgentProvider {

    /**
     * Returns the stable identifier used to select this provider from the agent runtime.
     *
     * @return non-blank provider identifier
     */
    String providerId();

    /**
     * Executes one provider-neutral agent request through this backend.
     *
     * @param request resolved execution request supplied by the agent runtime
     * @return provider-neutral execution result describing the completed backend attempt
     * @throws Exception when the backend cannot complete the request and no normal execution result
     *                   can be produced
     */
    AIAgentExecutionResult execute(AIAgentExecutionRequest request) throws Exception;
}

package org.tavall.ai.agent;

/** Provider adapter for an AI/model execution backend. */
public interface AIAgentProvider {
    String providerId();

    AIAgentExecutionResult execute(AIAgentExecutionRequest request) throws Exception;
}

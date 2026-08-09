package org.tavall.ai.agent.codex;

import org.tavall.ai.agent.AIAgentExecutionRequest;

import java.nio.file.Path;

@FunctionalInterface
public interface CodexWorkspaceResolver {
    Path resolve(AIAgentExecutionRequest request);
}

package org.tavall.ai.agent.codex;

import org.tavall.ai.agent.AIAgentExecutionRequest;

import java.nio.file.Path;

/** Resolves the already-authorized workspace lease for one Codex execution. */
@FunctionalInterface
public interface CodexWorkspaceResolver {
    Path resolve(AIAgentExecutionRequest request);
}

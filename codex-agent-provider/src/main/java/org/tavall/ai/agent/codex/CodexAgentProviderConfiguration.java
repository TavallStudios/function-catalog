package org.tavall.ai.agent.codex;

import java.nio.file.Path;
import java.util.Objects;

/** Host-side configuration for the Codex CLI provider. */
public record CodexAgentProviderConfiguration(
        Path executable,
        CodexSandboxMode sandboxMode
) {
    public CodexAgentProviderConfiguration {
        executable = Objects.requireNonNull(executable, "executable").toAbsolutePath().normalize();
        sandboxMode = Objects.requireNonNull(sandboxMode, "sandboxMode");
    }

    public static CodexAgentProviderConfiguration workspaceWrite(Path executable) {
        return new CodexAgentProviderConfiguration(executable, CodexSandboxMode.WORKSPACE_WRITE);
    }
}

package org.tavall.ai.agent.codex;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class CodexCommandBuilderTest {
    @Test
    void buildsFixedEphemeralWorkspaceWriteExecutionWithoutDangerousBypass() {
        CodexCommandBuilder builder = new CodexCommandBuilder(
                CodexAgentProviderConfiguration.workspaceWrite(Path.of("/usr/local/bin/codex"))
        );
        assertThat(builder.build(Path.of("/tmp/last-message.txt"))).containsExactly(
                "/usr/local/bin/codex", "-c", "approval_policy=\"never\"", "exec",
                "--sandbox", "workspace-write", "--ephemeral", "--ignore-user-config",
                "--json", "--color", "never", "--output-last-message", "/tmp/last-message.txt", "-"
        ).noneMatch(argument -> argument.contains("dangerously-bypass"));
    }

    @Test
    void supportsReadOnlyDelegatedWorkers() {
        CodexCommandBuilder builder = new CodexCommandBuilder(
                new CodexAgentProviderConfiguration(Path.of("/usr/local/bin/codex"), CodexSandboxMode.READ_ONLY)
        );
        assertThat(builder.build(Path.of("/tmp/result"))).containsSubsequence("--sandbox", "read-only");
    }
}

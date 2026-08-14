package org.tavall.ai.mcp.server;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RepositoryStagingMcpServerLauncherTest {
    @Test
    void injectsTheScopedRepositoryStagingRegistrarExplicitly() {
        assertThat(RepositoryStagingMcpServerLauncher.arguments(new String[] {
                "--state-file=/tmp/functions.json",
                "--snapshot-file=/tmp/functions-snapshot.json"
        })).containsExactly(
                "--registrar-class=org.tavall.ai.staging.RepositoryStagingRegistrar",
                "--state-file=/tmp/functions.json",
                "--snapshot-file=/tmp/functions-snapshot.json"
        );
    }

    @Test
    void preservesHelpWithoutInventingImplicitPackageScanning() {
        List<String> arguments = RepositoryStagingMcpServerLauncher.arguments(new String[] {"--help"});

        assertThat(arguments)
                .containsExactly(
                        "--registrar-class=org.tavall.ai.staging.RepositoryStagingRegistrar",
                        "--help"
                )
                .noneMatch(value -> value.startsWith("--scan="));
    }
}

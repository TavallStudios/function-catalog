package org.tavall.ai.agent.codex;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.tavall.ai.agent.AIAgentDefinition;
import org.tavall.ai.agent.AIAgentExecutionBudget;
import org.tavall.ai.agent.AIAgentExecutionRequest;
import org.tavall.ai.agent.AIAgentExecutionStatus;
import org.tavall.ai.agent.AIAgentJob;
import org.tavall.ai.core.catalog.AIFunctionCatalog;
import org.tavall.ai.core.catalog.AIFunctionCatalogView;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Duration;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CodexAgentProviderTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void executesInsideResolvedGitWorkspaceAndReturnsBoundedArtifacts() throws Exception {
        Path executable = fakeCodexExecutable();
        Path workspace = gitWorkspace("workspace");
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        AIFunctionCatalog catalog = new AIFunctionCatalog(objectMapper);
        AIAgentExecutionRequest request = request(catalog);
        CodexAgentProvider provider = new CodexAgentProvider(
                CodexAgentProviderConfiguration.workspaceWrite(executable),
                ignored -> workspace,
                localTestSupervisor(),
                objectMapper
        );

        var result = provider.execute(request);

        assertThat(result.status()).isEqualTo(AIAgentExecutionStatus.COMPLETED);
        assertThat(result.output().path("message").asText()).isEqualTo("fake codex completed");
        assertThat(result.output().path("eventsJsonl").asText()).contains("turn.completed");
        assertThat(result.output().path("exitCode").asInt()).isZero();
        assertThat(result.toolCalls()).isZero();
        try (var children = Files.list(workspace)) {
            assertThat(children.map(path -> path.getFileName().toString()).toList())
                    .noneMatch(name -> name.startsWith(".tavall-codex-"));
        }
    }

    @Test
    void rejectsWorkspaceThatIsNotAnExplicitGitRepositoryRoot() throws Exception {
        Path executable = fakeCodexExecutable();
        Path workspace = Files.createDirectory(temporaryDirectory.resolve("not-a-repo"));
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        AIFunctionCatalog catalog = new AIFunctionCatalog(objectMapper);
        CodexAgentProvider provider = new CodexAgentProvider(
                CodexAgentProviderConfiguration.workspaceWrite(executable),
                ignored -> workspace,
                localTestSupervisor(),
                objectMapper
        );

        assertThatThrownBy(() -> provider.execute(request(catalog)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("trusted Git repository");
    }

    @Test
    void inheritedEnvironmentIsExplicitlyAllowlisted() {
        Map<String, String> sanitized = CodexAgentProvider.sanitizedEnvironment(Map.of(
                "PATH", "/usr/bin",
                "OPENAI_API_KEY", "test-key",
                "TAVALL_CLOUD_CONTROL_HMAC", "sentinel-control-value",
                "DATABASE_PASSWORD", "sentinel-database-value"
        ));

        assertThat(sanitized)
                .containsEntry("PATH", "/usr/bin")
                .containsEntry("OPENAI_API_KEY", "test-key")
                .doesNotContainKeys("TAVALL_CLOUD_CONTROL_HMAC", "DATABASE_PASSWORD");
    }

    @Test
    void rejectsSupervisorThatBreaksTheCaptureContract() throws Exception {
        Path executable = fakeCodexExecutable();
        Path workspace = gitWorkspace("capture-contract-workspace");
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        AIFunctionCatalog catalog = new AIFunctionCatalog(objectMapper);
        String oversized = "x".repeat((256 * 1024) + 1);
        CodexAgentProvider provider = new CodexAgentProvider(
                CodexAgentProviderConfiguration.workspaceWrite(executable),
                ignored -> workspace,
                ignored -> new CodexProcessIsolationSupervisor.CodexSupervisedProcessResult(0, oversized, ""),
                objectMapper
        );

        assertThatThrownBy(() -> provider.execute(request(catalog)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("capture budget");
    }

    private AIAgentExecutionRequest request(AIFunctionCatalog catalog) {
        return new AIAgentExecutionRequest(
                new AIAgentDefinition(
                        "software-engineer",
                        "",
                        "Implement the requested change.",
                        CodexAgentProvider.PROVIDER_ID,
                        Set.of()
                ),
                new AIAgentJob("job-1", "edit the test fixture", 0, Map.of("repository", "fixture")),
                new AIAgentExecutionBudget(Duration.ofMinutes(1), 0, 0),
                new AIFunctionCatalogView(catalog, ignored -> false)
        );
    }

    /** Test-only adapter. Production workers must inject a host-isolated supervisor. */
    private CodexProcessIsolationSupervisor localTestSupervisor() {
        return request -> {
            ProcessBuilder processBuilder = new ProcessBuilder(request.command());
            processBuilder.directory(request.workspace().toFile());
            processBuilder.redirectInput(request.standardInput().toFile());
            processBuilder.environment().clear();
            processBuilder.environment().putAll(request.environment());
            Process process = processBuilder.start();
            String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            int exitCode = process.waitFor();
            return new CodexProcessIsolationSupervisor.CodexSupervisedProcessResult(exitCode, stdout, stderr);
        };
    }

    private Path gitWorkspace(String name) throws Exception {
        Path workspace = Files.createDirectory(temporaryDirectory.resolve(name));
        Process gitInit = new ProcessBuilder(
                "git", "init", "--quiet", "--initial-branch=main", workspace.toString()
        ).inheritIO().start();
        assertThat(gitInit.waitFor()).isZero();
        return workspace;
    }

    private Path fakeCodexExecutable() throws Exception {
        Path executable = temporaryDirectory.resolve("codex");
        Files.writeString(executable, """
                #!/usr/bin/env sh
                last=""
                previous=""
                for argument in "$@"; do
                  if [ "$previous" = "--output-last-message" ]; then
                    last="$argument"
                  fi
                  previous="$argument"
                done
                cat >/dev/null
                printf '%s\n' '{"type":"turn.completed"}'
                printf '%s' 'fake codex completed' > "$last"
                exit 0
                """);
        Files.setPosixFilePermissions(executable, PosixFilePermissions.fromString("rwx------"));
        return executable.toAbsolutePath().normalize();
    }
}

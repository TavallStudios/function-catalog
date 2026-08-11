package org.tavall.ai.agent.codex;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Host-owned process isolation boundary for Codex execution.
 *
 * <p>The Function Catalog provider deliberately does not implement OS process isolation itself.
 * A Tavall Cloud development worker must supply this boundary from its per-job supervisor. The
 * supervisor owns the execution identity and process group/cgroup from launch, must prevent the
 * Codex process and everything it starts from reading the worker JVM's process environment (for
 * example through {@code /proc}), and must not return until every process in the owned group is
 * terminated or otherwise proven quiescent.</p>
 *
 * <p>Implementations must also bound stdout and stderr while the process is running. Merely
 * truncating after execution is not sufficient.</p>
 */
@FunctionalInterface
public interface CodexProcessIsolationSupervisor {
    CodexSupervisedProcessResult execute(CodexSupervisedProcessRequest request) throws Exception;

    record CodexSupervisedProcessRequest(
            List<String> command,
            Path workspace,
            Path standardInput,
            Map<String, String> environment,
            int maximumCaptureBytes
    ) {
        public CodexSupervisedProcessRequest {
            command = List.copyOf(Objects.requireNonNull(command, "command"));
            workspace = Objects.requireNonNull(workspace, "workspace");
            standardInput = Objects.requireNonNull(standardInput, "standardInput");
            environment = Map.copyOf(Objects.requireNonNull(environment, "environment"));
            if (command.isEmpty()) {
                throw new IllegalArgumentException("command must not be empty");
            }
            if (!workspace.isAbsolute()) {
                throw new IllegalArgumentException("workspace must be absolute");
            }
            if (!standardInput.isAbsolute()) {
                throw new IllegalArgumentException("standardInput must be absolute");
            }
            if (maximumCaptureBytes <= 0) {
                throw new IllegalArgumentException("maximumCaptureBytes must be positive");
            }
        }
    }

    record CodexSupervisedProcessResult(int exitCode, String stdout, String stderr) {
        public CodexSupervisedProcessResult {
            stdout = Objects.requireNonNull(stdout, "stdout");
            stderr = Objects.requireNonNull(stderr, "stderr");
        }
    }
}

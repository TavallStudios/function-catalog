package org.tavall.ai.cloud;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.tavall.ai.core.catalog.AIFunctionCatalog;
import org.tavall.ai.core.catalog.AIFunctionDefinition;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

final class TavallCloudDeveloperFunctionsTest {
    @Test
    void publishesStableDeveloperCiAndRunnerFunctionNames() {
        AIFunctionCatalog catalog = new AIFunctionCatalog(new ObjectMapper());
        catalog.registerInstances(new FakeTavallCloudDeveloperFunctions());

        Map<String, AIFunctionDefinition> definitions = catalog.getFunctionDefinitions();
        assertThat(definitions.keySet()).containsExactlyInAnyOrder(
                "cloud_dev_ci_start",
                "cloud_dev_ci_inspect",
                "cloud_dev_ci_cancel",
                "cloud_dev_ci_evidence",
                "cloud_dev_tool_exec",
                "cloud_dev_github_exec"
        );

        assertThat(definitions.get("cloud_dev_ci_start").getRequiredParameters()).containsExactly(
                "repository",
                "workspaceId",
                "branch",
                "expectedHead",
                "profile",
                "origin",
                "provider"
        );
        assertThat(definitions.get("cloud_dev_tool_exec").getRequiredParameters()).containsExactly(
                "operationId",
                "tool",
                "arguments",
                "timeoutSeconds"
        );
    }

    private static final class FakeTavallCloudDeveloperFunctions implements TavallCloudDeveloperFunctions {
        @Override
        public TavallCloudOperationResult startCi(
                String repository,
                String workspaceId,
                String branch,
                String expectedHead,
                TavallCloudCiProfile profile,
                TavallCloudCiOrigin origin,
                String provider
        ) {
            return result("ci-start");
        }

        @Override
        public TavallCloudOperationResult inspectCi(String jobId) {
            return result(jobId);
        }

        @Override
        public TavallCloudOperationResult cancelCi(String jobId) {
            return result(jobId);
        }

        @Override
        public TavallCloudOperationResult ciEvidence(String jobId) {
            return result(jobId);
        }

        @Override
        public TavallCloudOperationResult executeTool(
                String operationId,
                TavallCloudDeveloperTool tool,
                List<String> arguments,
                int timeoutSeconds
        ) {
            return result(operationId);
        }

        @Override
        public TavallCloudOperationResult executeGithub(
                String workspaceId,
                String operation,
                List<String> arguments,
                int timeoutSeconds
        ) {
            return result(workspaceId);
        }

        private TavallCloudOperationResult result(String operationId) {
            return new TavallCloudOperationResult(
                    true,
                    operationId,
                    "QUEUED",
                    "tavall-storage://dev-storage/jobs/1c057688-91d4-431f-a99b-b32ebefb983b",
                    "accepted"
            );
        }
    }
}

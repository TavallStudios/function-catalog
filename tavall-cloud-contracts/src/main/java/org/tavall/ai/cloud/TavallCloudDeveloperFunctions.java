package org.tavall.ai.cloud;

import org.tavall.ai.core.annotation.AIFunction;
import org.tavall.ai.core.annotation.AIParam;

import java.util.List;

/**
 * Stable Function Catalog contract for Tavall Cloud development execution.
 * Implementations live in Tavall Cloud adapters; this module owns names and schemas only.
 */
public interface TavallCloudDeveloperFunctions {
    @AIFunction(
            name = "cloud_dev_ci_start",
            description = "Start exact-head Tavall local CI through DEVELOPMENT CONTROL and an authorized sandbox provider."
    )
    TavallCloudOperationResult startCi(
            @AIParam(name = "repository", description = "Canonical owner/repository identity.") String repository,
            @AIParam(name = "workspaceId", description = "Leased Tavall developer workspace identity.") String workspaceId,
            @AIParam(name = "branch", description = "Workspace branch associated with the requested exact head.") String branch,
            @AIParam(name = "expectedHead", description = "Immutable lowercase Git commit identifier that CI must verify before execution.") String expectedHead,
            @AIParam(name = "profile", description = "Repository-owned CI profile.") TavallCloudCiProfile profile,
            @AIParam(name = "origin", description = "Tavall control surface requesting this CI run.") TavallCloudCiOrigin origin,
            @AIParam(name = "provider", description = "Requested Tavall sandbox execution provider.") String provider
    );

    @AIFunction(
            name = "cloud_dev_ci_inspect",
            description = "Inspect a durable Tavall local CI job without changing it."
    )
    TavallCloudOperationResult inspectCi(
            @AIParam(name = "jobId", description = "Durable Tavall developer CI job identifier.") String jobId
    );

    @AIFunction(
            name = "cloud_dev_ci_cancel",
            description = "Cancel a durable Tavall local CI job through DEVELOPMENT CONTROL."
    )
    TavallCloudOperationResult cancelCi(
            @AIParam(name = "jobId", description = "Durable Tavall developer CI job identifier.") String jobId
    );

    @AIFunction(
            name = "cloud_dev_ci_evidence",
            description = "Resolve durable DEV STORAGE evidence metadata for a Tavall local CI job."
    )
    TavallCloudOperationResult ciEvidence(
            @AIParam(name = "jobId", description = "Durable Tavall developer CI job identifier.") String jobId
    );

    @AIFunction(
            name = "cloud_dev_tool_exec",
            description = "Execute an explicitly authorized development tool inside a Tavall sandbox. Raw shell is a stronger trusted-only tool class."
    )
    TavallCloudOperationResult executeTool(
            @AIParam(name = "operationId", description = "Authorized sandbox operation identifier.") String operationId,
            @AIParam(name = "tool", description = "Development tool class to execute.") TavallCloudDeveloperTool tool,
            @AIParam(name = "arguments", description = "Exact argument vector passed to the selected tool, not a shell-expanded string.") List<String> arguments,
            @AIParam(name = "timeoutSeconds", description = "Bounded execution timeout in seconds.") int timeoutSeconds
    );

    @AIFunction(
            name = "cloud_dev_github_exec",
            description = "Execute an explicitly authorized bounded GitHub or gh operation in a leased Tavall developer workspace."
    )
    TavallCloudOperationResult executeGithub(
            @AIParam(name = "workspaceId", description = "Leased Tavall developer workspace identity.") String workspaceId,
            @AIParam(name = "operation", description = "Allowlisted GitHub operation name.") String operation,
            @AIParam(name = "arguments", description = "Exact bounded GitHub operation arguments.") List<String> arguments,
            @AIParam(name = "timeoutSeconds", description = "Bounded execution timeout in seconds.") int timeoutSeconds
    );
}

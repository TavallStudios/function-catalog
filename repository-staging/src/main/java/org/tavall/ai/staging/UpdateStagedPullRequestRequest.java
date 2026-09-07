package org.tavall.ai.staging;

import java.util.Optional;

/** A fenced PR topology/metadata update, preserving ancestry for all descendants. */
public record UpdateStagedPullRequestRequest(
        RepositoryCoordinates repository,
        int pullRequestNumber,
        String expectedHeadSha,
        Optional<String> baseBranch,
        Optional<String> body
) {
    public UpdateStagedPullRequestRequest {
        if (repository == null || pullRequestNumber < 1) {
            throw new IllegalArgumentException("repository and a positive pull request number are required");
        }
        if (expectedHeadSha == null || !expectedHeadSha.matches("[a-f0-9]{40}|[a-f0-9]{64}")) {
            throw new IllegalArgumentException("expectedHeadSha must be an exact commit SHA");
        }
        baseBranch = baseBranch == null ? Optional.empty() : baseBranch;
        body = body == null ? Optional.empty() : body;
    }
}

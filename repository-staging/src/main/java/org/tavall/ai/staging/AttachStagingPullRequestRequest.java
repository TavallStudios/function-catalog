package org.tavall.ai.staging;

public record AttachStagingPullRequestRequest(
        RepositoryCoordinates repository,
        int pullRequestNumber,
        int stagingPullRequestNumber
) {
    public AttachStagingPullRequestRequest {
        if (repository == null) throw new IllegalArgumentException("repository must not be null");
        if (pullRequestNumber < 1 || stagingPullRequestNumber < 1) throw new IllegalArgumentException("pull request numbers must be positive");
    }
}

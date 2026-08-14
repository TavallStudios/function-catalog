package org.tavall.ai.staging;

public record StagingPullRequest(RepositoryPullRequest pullRequest, StagingMetadata metadata) {
    public StagingPullRequest {
        if (pullRequest == null || metadata == null) throw new IllegalArgumentException("pullRequest/metadata must not be null");
    }
}

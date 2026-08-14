package org.tavall.ai.staging;

public record PrepareStagingPromotionRequest(RepositoryCoordinates repository, int stagingPullRequestNumber) {
    public PrepareStagingPromotionRequest {
        if (repository == null) throw new IllegalArgumentException("repository must not be null");
        if (stagingPullRequestNumber < 1) throw new IllegalArgumentException("stagingPullRequestNumber must be positive");
    }
}

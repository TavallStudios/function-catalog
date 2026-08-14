package org.tavall.ai.staging;

public record ResolveStagingBaseRequest(
        RepositoryCoordinates repository,
        Integer currentPullRequestNumber,
        Integer dependentOnPullRequestNumber,
        StagingType preferredStagingType,
        String preferredStagingBranch
) {
    public ResolveStagingBaseRequest {
        if (repository == null) throw new IllegalArgumentException("repository must not be null");
        preferredStagingBranch = preferredStagingBranch == null || preferredStagingBranch.isBlank()
                ? null : preferredStagingBranch.trim();
    }
}

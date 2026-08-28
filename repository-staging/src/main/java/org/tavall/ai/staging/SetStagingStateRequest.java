package org.tavall.ai.staging;

public record SetStagingStateRequest(
        RepositoryCoordinates repository,
        int stagingPullRequestNumber,
        StagingState state,
        StagingExecutionContext context
) {
    public SetStagingStateRequest(
            RepositoryCoordinates repository,
            int stagingPullRequestNumber,
            StagingState state
    ) {
        this(repository, stagingPullRequestNumber, state, null);
    }

    public SetStagingStateRequest {
        if (repository == null || state == null) throw new IllegalArgumentException("repository/state must not be null");
        if (stagingPullRequestNumber < 1) throw new IllegalArgumentException("stagingPullRequestNumber must be positive");
    }
}

package org.tavall.ai.staging;

public record StagingStateResult(int stagingPullRequestNumber, StagingState previousState, StagingState state) {}

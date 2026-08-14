package org.tavall.ai.staging;

public record StagingAttachResult(boolean attached, int pullRequestNumber, int stagingPullRequestNumber, String baseBranch) {}

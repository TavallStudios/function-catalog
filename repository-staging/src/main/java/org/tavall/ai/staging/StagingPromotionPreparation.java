package org.tavall.ai.staging;

import java.util.List;

public record StagingPromotionPreparation(
        int stagingPullRequestNumber,
        String exactHeadSha,
        StagingState state,
        boolean blocked,
        List<Integer> openChildPullRequests,
        List<RepositoryCheck> checks,
        List<StagingTopologyFinding> findings,
        List<String> blockers
) {
    public StagingPromotionPreparation {
        openChildPullRequests = List.copyOf(openChildPullRequests);
        checks = List.copyOf(checks);
        findings = List.copyOf(findings);
        blockers = List.copyOf(blockers);
    }
}

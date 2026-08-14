package org.tavall.ai.staging;

import java.util.List;

public record StagingBaseResolution(
        boolean resolved,
        String baseBranch,
        Integer parentPullRequestNumber,
        StagingBaseReason reason,
        List<Integer> candidatePullRequestNumbers,
        List<StagingTopologyFinding> findings
) {
    public StagingBaseResolution {
        baseBranch = baseBranch == null ? "" : baseBranch;
        if (reason == null) throw new IllegalArgumentException("reason must not be null");
        candidatePullRequestNumbers = List.copyOf(candidatePullRequestNumbers);
        findings = List.copyOf(findings);
    }
}

package org.tavall.ai.staging;

import java.util.List;

public record StagingDiscoveryResult(List<StagingPullRequest> stagingPullRequests, List<StagingTopologyFinding> findings) {
    public StagingDiscoveryResult {
        stagingPullRequests = List.copyOf(stagingPullRequests);
        findings = List.copyOf(findings);
    }
}

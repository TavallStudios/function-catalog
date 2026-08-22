package org.tavall.ai.staging;

import java.util.List;

public record StagingGraph(
        List<RepositoryPullRequest> pullRequests,
        List<StagingPullRequest> stagingPullRequests,
        List<StagingRelationship> relationships,
        List<StagingTopologyFinding> findings,
        StagingExecutionEvidence executionEvidence
) {
    public StagingGraph(
            List<RepositoryPullRequest> pullRequests,
            List<StagingPullRequest> stagingPullRequests,
            List<StagingRelationship> relationships,
            List<StagingTopologyFinding> findings
    ) {
        this(pullRequests, stagingPullRequests, relationships, findings, null);
    }

    public StagingGraph {
        pullRequests = List.copyOf(pullRequests);
        stagingPullRequests = List.copyOf(stagingPullRequests);
        relationships = List.copyOf(relationships);
        findings = List.copyOf(findings);
    }
}

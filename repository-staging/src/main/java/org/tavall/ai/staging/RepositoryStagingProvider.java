package org.tavall.ai.staging;

import java.util.List;
import java.util.Optional;

/** Repository I/O boundary. Staging semantics live in RepositoryStagingService, not the provider. */
public interface RepositoryStagingProvider {
    /**
     * Returns a provider bound to one exact execution context. Providers that
     * do not need an execution fence may return {@code this}.
     */
    default RepositoryStagingProvider scoped(StagingExecutionContext context) {
        return this;
    }

    /** Returns the exact execution evidence for the current scoped provider, when available. */
    default Optional<StagingExecutionEvidence> executionEvidence() {
        return Optional.empty();
    }

    List<RepositoryPullRequest> listOpenPullRequests(RepositoryCoordinates repository);
    Optional<String> branchHead(RepositoryCoordinates repository, String branch);
    void createBranch(RepositoryCoordinates repository, String branch, String sha);
    RepositoryPullRequest createPullRequest(
            RepositoryCoordinates repository,
            String title,
            String headBranch,
            String baseBranch,
            String body,
            boolean draft
    );
    RepositoryPullRequest updatePullRequest(
            RepositoryCoordinates repository,
            int pullRequestNumber,
            Optional<String> baseBranch,
            Optional<String> body
    );
    List<RepositoryCheck> checksForHead(RepositoryCoordinates repository, String headSha);
}

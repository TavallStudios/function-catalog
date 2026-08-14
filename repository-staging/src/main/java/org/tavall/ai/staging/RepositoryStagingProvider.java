package org.tavall.ai.staging;

import java.util.List;
import java.util.Optional;

/** Repository I/O boundary. Staging semantics live in RepositoryStagingService, not the provider. */
public interface RepositoryStagingProvider {
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

package org.tavall.ai.review;

import java.util.List;
import java.util.Objects;

public final class RepositoryReviewService {
    private final RepositoryReviewProvider provider;

    public RepositoryReviewService(RepositoryReviewProvider provider) {
        this.provider = Objects.requireNonNull(provider, "provider");
    }

    public RepositoryReviewSnapshot inspectPullRequest(InspectPullRequestRequest request) {
        Objects.requireNonNull(request, "request");
        return provider.inspectPullRequest(request.repository(), request.pullRequestNumber());
    }

    public RepositoryReviewSnapshot inspectRange(InspectRepositoryRangeRequest request) {
        Objects.requireNonNull(request, "request");
        return provider.inspectRange(request.repository(), request.base(), request.head());
    }

    public List<RepositoryReviewThread> listThreads(ListRepositoryReviewThreadsRequest request) {
        Objects.requireNonNull(request, "request");
        return provider.listReviewThreads(request.repository(), request.pullRequestNumber());
    }

    public RepositoryPublishedReview publish(PublishRepositoryReviewRequest request) {
        Objects.requireNonNull(request, "request");
        RepositoryReviewSnapshot current = provider.inspectPullRequest(request.repository(), request.pullRequestNumber());
        if (!request.exactHeadSha().equals(current.exactHeadSha())) {
            throw new IllegalStateException("Pull request head moved: expected " + request.exactHeadSha()
                    + " but GitHub reports " + current.exactHeadSha());
        }
        return provider.publishReview(request);
    }
}

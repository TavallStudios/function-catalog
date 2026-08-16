package org.tavall.ai.review;

import java.util.List;

public interface RepositoryReviewProvider {
    RepositoryReviewSnapshot inspectPullRequest(RepositoryReviewCoordinates repository, int pullRequestNumber);
    RepositoryReviewSnapshot inspectRange(RepositoryReviewCoordinates repository, String base, String head);
    List<RepositoryReviewThread> listReviewThreads(RepositoryReviewCoordinates repository, int pullRequestNumber);
    RepositoryPublishedReview publishReview(PublishRepositoryReviewRequest request);
}

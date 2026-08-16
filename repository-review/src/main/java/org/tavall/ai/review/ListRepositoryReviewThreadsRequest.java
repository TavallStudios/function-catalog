package org.tavall.ai.review;

import java.util.Objects;

public record ListRepositoryReviewThreadsRequest(RepositoryReviewCoordinates repository, int pullRequestNumber) {
    public ListRepositoryReviewThreadsRequest {
        Objects.requireNonNull(repository, "repository");
        if (pullRequestNumber <= 0) throw new IllegalArgumentException("pullRequestNumber must be positive");
    }
}

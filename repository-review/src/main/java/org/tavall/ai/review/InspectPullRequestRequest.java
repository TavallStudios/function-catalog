package org.tavall.ai.review;

import java.util.Objects;

public record InspectPullRequestRequest(RepositoryReviewCoordinates repository, int pullRequestNumber) {
    public InspectPullRequestRequest {
        Objects.requireNonNull(repository, "repository");
        if (pullRequestNumber <= 0) throw new IllegalArgumentException("pullRequestNumber must be positive");
    }
}

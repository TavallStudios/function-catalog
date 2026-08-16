package org.tavall.ai.review;

public record RepositoryPublishedReview(long reviewId, int pullRequestNumber, String exactHeadSha, RepositoryReviewDecision decision) { }

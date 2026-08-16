package org.tavall.ai.review;

import java.util.List;
import java.util.Objects;

public record PublishRepositoryReviewRequest(
        RepositoryReviewCoordinates repository,
        int pullRequestNumber,
        String exactHeadSha,
        RepositoryReviewDecision decision,
        String summary,
        List<RepositoryReviewComment> comments
) {
    public PublishRepositoryReviewRequest {
        Objects.requireNonNull(repository, "repository");
        if (pullRequestNumber <= 0) throw new IllegalArgumentException("pullRequestNumber must be positive");
        exactHeadSha = requireText(exactHeadSha, "exactHeadSha");
        Objects.requireNonNull(decision, "decision");
        summary = summary == null ? "" : summary.trim();
        comments = List.copyOf(comments == null ? List.of() : comments);
        if (decision != RepositoryReviewDecision.APPROVE && summary.isBlank()) {
            throw new IllegalArgumentException("summary is required for COMMENT or REQUEST_CHANGES");
        }
    }

    private static String requireText(String value, String field) {
        String safe = Objects.requireNonNull(value, field).trim();
        if (safe.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        return safe;
    }
}

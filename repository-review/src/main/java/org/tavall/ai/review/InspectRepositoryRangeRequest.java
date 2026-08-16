package org.tavall.ai.review;

import java.util.Objects;

public record InspectRepositoryRangeRequest(RepositoryReviewCoordinates repository, String base, String head) {
    public InspectRepositoryRangeRequest {
        Objects.requireNonNull(repository, "repository");
        base = requireText(base, "base");
        head = requireText(head, "head");
    }

    private static String requireText(String value, String field) {
        String safe = Objects.requireNonNull(value, field).trim();
        if (safe.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        return safe;
    }
}

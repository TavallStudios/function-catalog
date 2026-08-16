package org.tavall.ai.review;

import java.util.Objects;

public record RepositoryReviewComment(String path, int line, String side, String body) {
    public RepositoryReviewComment {
        path = requireText(path, "path");
        if (line <= 0) throw new IllegalArgumentException("line must be positive");
        side = requireText(side, "side").toUpperCase();
        if (!side.equals("LEFT") && !side.equals("RIGHT")) throw new IllegalArgumentException("side must be LEFT or RIGHT");
        body = requireText(body, "body");
    }

    private static String requireText(String value, String field) {
        String safe = Objects.requireNonNull(value, field).trim();
        if (safe.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        return safe;
    }
}

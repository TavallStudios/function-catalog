package org.tavall.ai.review;

import java.util.Objects;

public record RepositoryReviewCoordinates(String owner, String name) {
    public RepositoryReviewCoordinates {
        owner = requirePart(owner, "owner");
        name = requirePart(name, "name");
    }

    public static RepositoryReviewCoordinates parse(String fullName) {
        String value = Objects.requireNonNull(fullName, "fullName").trim();
        String[] parts = value.split("/", -1);
        if (parts.length != 2) throw new IllegalArgumentException("Repository must be owner/name: " + fullName);
        return new RepositoryReviewCoordinates(parts[0], parts[1]);
    }

    public String fullName() { return owner + "/" + name; }

    private static String requirePart(String value, String field) {
        String safe = Objects.requireNonNull(value, field).trim();
        if (safe.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        return safe;
    }
}

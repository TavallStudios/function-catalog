package org.tavall.ai.staging;

public record RepositoryCoordinates(String owner, String name) {
    public RepositoryCoordinates {
        owner = requireText(owner, "owner");
        name = requireText(name, "name");
    }

    public static RepositoryCoordinates parse(String value) {
        String safe = requireText(value, "repository");
        String[] parts = safe.split("/", -1);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Repository must use owner/name form: " + value);
        }
        return new RepositoryCoordinates(parts[0], parts[1]);
    }

    public String fullName() {
        return owner + "/" + name;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(fieldName + " must not be blank");
        return value.trim();
    }
}

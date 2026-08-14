package org.tavall.ai.staging;

public record RepositoryCheck(String name, RepositoryCheckStatus status, String conclusion) {
    public RepositoryCheck {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name must not be blank");
        name = name.trim();
        if (status == null) throw new IllegalArgumentException("status must not be null");
        conclusion = conclusion == null ? "" : conclusion.trim();
    }
}

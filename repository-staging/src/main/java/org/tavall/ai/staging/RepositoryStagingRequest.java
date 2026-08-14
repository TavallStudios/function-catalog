package org.tavall.ai.staging;

public record RepositoryStagingRequest(RepositoryCoordinates repository) {
    public RepositoryStagingRequest {
        if (repository == null) throw new IllegalArgumentException("repository must not be null");
    }
}

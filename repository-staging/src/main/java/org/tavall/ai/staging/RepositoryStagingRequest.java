package org.tavall.ai.staging;

public record RepositoryStagingRequest(RepositoryCoordinates repository, StagingExecutionContext context) {
    public RepositoryStagingRequest(RepositoryCoordinates repository) {
        this(repository, null);
    }

    public RepositoryStagingRequest {
        if (repository == null) throw new IllegalArgumentException("repository must not be null");
    }
}

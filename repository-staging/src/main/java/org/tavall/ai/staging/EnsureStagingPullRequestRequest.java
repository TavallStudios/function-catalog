package org.tavall.ai.staging;

public record EnsureStagingPullRequestRequest(
        RepositoryCoordinates repository,
        StagingType type,
        String title,
        String branch,
        String parentBranch
) {
    public EnsureStagingPullRequestRequest {
        if (repository == null || type == null) throw new IllegalArgumentException("repository/type must not be null");
        title = text(title, "title");
        branch = text(branch, "branch");
        parentBranch = text(parentBranch, "parentBranch");
    }
    private static String text(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        return value.trim();
    }
}

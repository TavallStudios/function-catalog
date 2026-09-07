package org.tavall.ai.staging;

/** A focused PR creation whose proposed dependency graph must be valid before publication. */
public record CreateStagedPullRequestRequest(
        RepositoryCoordinates repository,
        String title,
        String body,
        String headBranch,
        String expectedHeadSha,
        String baseBranch,
        boolean draft
) {
    public CreateStagedPullRequestRequest {
        if (repository == null) throw new IllegalArgumentException("repository must not be null");
        if (expectedHeadSha == null || !expectedHeadSha.matches("[a-f0-9]{40}|[a-f0-9]{64}")) {
            throw new IllegalArgumentException("expectedHeadSha must be an exact commit SHA");
        }
        if (title == null || title.isBlank() || headBranch == null || headBranch.isBlank()
                || baseBranch == null || baseBranch.isBlank()) {
            throw new IllegalArgumentException("title, headBranch and baseBranch are required");
        }
        body = body == null ? "" : body;
    }
}

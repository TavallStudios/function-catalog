package org.tavall.ai.staging;

public record RepositoryPullRequest(
        int number,
        String title,
        String body,
        String headBranch,
        String headSha,
        String baseBranch,
        String baseSha,
        boolean draft
) {
    public RepositoryPullRequest {
        if (number < 1) throw new IllegalArgumentException("number must be positive");
        title = text(title);
        body = body == null ? "" : body;
        headBranch = text(headBranch);
        headSha = text(headSha);
        baseBranch = text(baseBranch);
        baseSha = baseSha == null ? "" : baseSha.trim();
    }

    private static String text(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Pull request text must not be blank");
        return value.trim();
    }
}

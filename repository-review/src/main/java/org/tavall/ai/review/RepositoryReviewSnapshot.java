package org.tavall.ai.review;

import java.util.List;
import java.util.Objects;

public record RepositoryReviewSnapshot(
        RepositoryReviewCoordinates repository,
        int pullRequestNumber,
        String baseRef,
        String baseSha,
        String headRef,
        String exactHeadSha,
        String diff,
        List<String> changedFiles
) {
    public RepositoryReviewSnapshot {
        Objects.requireNonNull(repository, "repository");
        baseRef = Objects.requireNonNull(baseRef, "baseRef");
        baseSha = Objects.requireNonNull(baseSha, "baseSha");
        headRef = Objects.requireNonNull(headRef, "headRef");
        exactHeadSha = Objects.requireNonNull(exactHeadSha, "exactHeadSha");
        diff = Objects.requireNonNull(diff, "diff");
        changedFiles = List.copyOf(changedFiles == null ? List.of() : changedFiles);
    }
}

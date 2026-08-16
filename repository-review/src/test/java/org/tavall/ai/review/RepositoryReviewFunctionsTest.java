package org.tavall.ai.review;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class RepositoryReviewFunctionsTest {

    @Test
    void inspectPullRequestReturnsCompleteExactHeadSnapshot() {
        RecordingProvider provider = new RecordingProvider();
        RepositoryReviewFunctions functions = new RepositoryReviewFunctions(new RepositoryReviewService(provider));
        RepositoryReviewCoordinates repository = RepositoryReviewCoordinates.parse("TavallStudios/example");

        RepositoryReviewSnapshot snapshot = functions.inspectPullRequest(new InspectPullRequestRequest(repository, 17));

        assertEquals("head-17", snapshot.exactHeadSha());
        assertEquals("base-17", snapshot.baseSha());
        assertEquals("main", snapshot.baseRef());
        assertEquals("feature/review-me", snapshot.headRef());
        assertEquals(List.of("src/A.java", "src/B.java"), snapshot.changedFiles());
        assertEquals(17, provider.lastPullRequestNumber);
    }

    @Test
    void publishRefusesToReviewAHeadThatMovedSinceAnalysis() {
        RecordingProvider provider = new RecordingProvider();
        RepositoryReviewFunctions functions = new RepositoryReviewFunctions(new RepositoryReviewService(provider));
        RepositoryReviewCoordinates repository = RepositoryReviewCoordinates.parse("TavallStudios/example");

        PublishRepositoryReviewRequest request = new PublishRepositoryReviewRequest(
                repository,
                17,
                "stale-head",
                RepositoryReviewDecision.REQUEST_CHANGES,
                "Blocking correctness regression.",
                List.of(new RepositoryReviewComment("src/A.java", 12, "RIGHT", "The failure is swallowed."))
        );

        IllegalStateException error = assertThrows(IllegalStateException.class, () -> functions.publish(request));
        assertEquals("Pull request head moved: expected stale-head but GitHub reports head-17", error.getMessage());
        assertEquals(0, provider.publishCount);
    }

    @Test
    void approvalRequiresAReviewBody() {
        RepositoryReviewCoordinates repository = RepositoryReviewCoordinates.parse("TavallStudios/example");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () ->
                new PublishRepositoryReviewRequest(
                        repository,
                        17,
                        "head-17",
                        RepositoryReviewDecision.APPROVE,
                        "   ",
                        List.of()
                ));

        assertEquals("summary must not be blank", error.getMessage());
    }

    private static final class RecordingProvider implements RepositoryReviewProvider {
        private int lastPullRequestNumber;
        private int publishCount;

        @Override
        public RepositoryReviewSnapshot inspectPullRequest(RepositoryReviewCoordinates repository, int pullRequestNumber) {
            lastPullRequestNumber = pullRequestNumber;
            return new RepositoryReviewSnapshot(repository, pullRequestNumber, "main", "base-17",
                    "feature/review-me", "head-17", "diff --git a/src/A.java b/src/A.java\n+changed",
                    List.of("src/A.java", "src/B.java"));
        }

        @Override
        public RepositoryReviewSnapshot inspectRange(RepositoryReviewCoordinates repository, String base, String head) {
            return new RepositoryReviewSnapshot(repository, 0, base, "base-sha", head, "head-sha", "diff", List.of());
        }

        @Override
        public List<RepositoryReviewThread> listReviewThreads(RepositoryReviewCoordinates repository, int pullRequestNumber) {
            return List.of();
        }

        @Override
        public RepositoryPublishedReview publishReview(PublishRepositoryReviewRequest request) {
            publishCount++;
            return new RepositoryPublishedReview(99L, request.pullRequestNumber(), request.exactHeadSha(), request.decision());
        }
    }
}

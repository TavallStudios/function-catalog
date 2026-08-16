package org.tavall.ai.review;

import org.tavall.ai.core.annotation.AIFunction;
import org.tavall.ai.core.annotation.AIParam;

import java.util.List;
import java.util.Objects;

/** Repo-agnostic GitHub review transport functions consumed by Tavall review agents and ChatGPT Web. */
public final class RepositoryReviewFunctions {
    private final RepositoryReviewService service;

    public RepositoryReviewFunctions(RepositoryReviewService service) {
        this.service = Objects.requireNonNull(service, "service");
    }

    @AIFunction(name = "github_inspect_pr", description = "Inspect a pull request as an exact-head review snapshot including refs, diff, and changed files.")
    public RepositoryReviewSnapshot inspectPullRequest(@AIParam(name = "request") InspectPullRequestRequest request) {
        return service.inspectPullRequest(request);
    }

    @AIFunction(name = "github_inspect_ref_range", description = "Inspect an arbitrary repository base/head range resolved to exact commit SHAs.")
    public RepositoryReviewSnapshot inspectRange(@AIParam(name = "request") InspectRepositoryRangeRequest request) {
        return service.inspectRange(request);
    }

    @AIFunction(name = "github_list_review_threads", description = "List existing inline pull-request review comments for finding reconciliation.")
    public List<RepositoryReviewThread> listThreads(@AIParam(name = "request") ListRepositoryReviewThreadsRequest request) {
        return service.listThreads(request);
    }

    @AIFunction(name = "github_review_pr", description = "Publish an exact-head-anchored GitHub review; refuses publication when the pull-request head moved.")
    public RepositoryPublishedReview publish(@AIParam(name = "request") PublishRepositoryReviewRequest request) {
        return service.publish(request);
    }
}

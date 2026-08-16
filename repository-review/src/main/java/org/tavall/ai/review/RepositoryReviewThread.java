package org.tavall.ai.review;

public record RepositoryReviewThread(long id, String path, int line, String body, Long inReplyToId, boolean outdated) { }

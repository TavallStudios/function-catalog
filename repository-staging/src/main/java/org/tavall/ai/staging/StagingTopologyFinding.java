package org.tavall.ai.staging;

public record StagingTopologyFinding(
        String code,
        StagingFindingSeverity severity,
        String message,
        Integer pullRequestNumber
) {
    public StagingTopologyFinding {
        if (code == null || code.isBlank()) throw new IllegalArgumentException("code must not be blank");
        if (severity == null) throw new IllegalArgumentException("severity must not be null");
        if (message == null || message.isBlank()) throw new IllegalArgumentException("message must not be blank");
    }
}

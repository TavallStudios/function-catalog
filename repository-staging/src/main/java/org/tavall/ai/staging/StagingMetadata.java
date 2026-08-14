package org.tavall.ai.staging;

public record StagingMetadata(
        StagingType type,
        StagingState state,
        String branch,
        String parent,
        String promotion,
        String childMergeTarget
) {
    public StagingMetadata {
        if (type == null || state == null) throw new IllegalArgumentException("type/state must not be null");
        branch = text(branch, "branch");
        parent = text(parent, "parent");
        promotion = text(promotion, "promotion");
        childMergeTarget = text(childMergeTarget, "childMergeTarget");
    }

    private static String text(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        return value.trim();
    }
}

package org.tavall.ai.staging;

import java.util.Optional;

public record StagingMetadata(
        StagingType type,
        StagingState state,
        String branch,
        String parent,
        String promotion,
        String childMergeTarget,
        Optional<String> lifecycle,
        Optional<String> runtimeId,
        Optional<String> runtimeStack,
        Optional<String> fanInMode,
        Optional<String> runtimeFlags,
        Optional<String> architectureProfile,
        Optional<String> architectureCheck
) {
    public StagingMetadata(
            StagingType type,
            StagingState state,
            String branch,
            String parent,
            String promotion,
            String childMergeTarget
    ) {
        this(
                type,
                state,
                branch,
                parent,
                promotion,
                childMergeTarget,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty()
        );
    }

    public StagingMetadata {
        if (type == null || state == null) throw new IllegalArgumentException("type/state must not be null");
        branch = text(branch, "branch");
        parent = text(parent, "parent");
        promotion = text(promotion, "promotion");
        childMergeTarget = text(childMergeTarget, "childMergeTarget");
        lifecycle = optional(lifecycle, "lifecycle");
        runtimeId = optional(runtimeId, "runtimeId");
        runtimeStack = optional(runtimeStack, "runtimeStack");
        fanInMode = optional(fanInMode, "fanInMode");
        runtimeFlags = optional(runtimeFlags, "runtimeFlags");
        architectureProfile = optional(architectureProfile, "architectureProfile");
        architectureCheck = optional(architectureCheck, "architectureCheck");
    }

    private static String text(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        return value.trim();
    }

    private static Optional<String> optional(Optional<String> value, String field) {
        if (value == null) throw new IllegalArgumentException(field + " must not be null");
        return value.filter(text -> !text.isBlank()).map(String::trim);
    }
}

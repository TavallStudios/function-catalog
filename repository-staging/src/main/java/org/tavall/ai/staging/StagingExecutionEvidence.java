package org.tavall.ai.staging;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/** Exact Cloud environment evidence attached to a staging graph inspection. */
public record StagingExecutionEvidence(
        String laneId,
        String environmentId,
        long environmentGeneration,
        long leaseGeneration,
        String sourceSnapshotDigest,
        String desiredState,
        String observedState,
        String latestValidationResult,
        String latestValidationProfile,
        String latestValidationSourceSnapshotDigest,
        String latestValidationEvidenceHandle
) {
    private static final Pattern SHA256 = Pattern.compile("[a-f0-9]{64}");

    public StagingExecutionEvidence {
        laneId = text(laneId, "laneId");
        environmentId = text(environmentId, "environmentId");
        if (environmentGeneration <= 0 || leaseGeneration <= 0) {
            throw new IllegalArgumentException("environment generations must be positive");
        }
        sourceSnapshotDigest = digest(sourceSnapshotDigest, "sourceSnapshotDigest");
        desiredState = text(desiredState, "desiredState");
        observedState = text(observedState, "observedState");
        latestValidationResult = Objects.requireNonNullElse(latestValidationResult, "").strip();
        latestValidationProfile = Objects.requireNonNullElse(latestValidationProfile, "").strip();
        latestValidationSourceSnapshotDigest = Objects.requireNonNullElse(
                latestValidationSourceSnapshotDigest,
                ""
        ).strip().toLowerCase(Locale.ROOT);
        if (!latestValidationSourceSnapshotDigest.isBlank()
                && !SHA256.matcher(latestValidationSourceSnapshotDigest).matches()) {
            throw new IllegalArgumentException("latestValidationSourceSnapshotDigest must be blank or SHA-256");
        }
        latestValidationEvidenceHandle = Objects.requireNonNullElse(latestValidationEvidenceHandle, "").strip();
    }

    private static String text(String value, String field) {
        String normalized = Objects.requireNonNullElse(value, "").strip();
        if (normalized.isBlank() || normalized.indexOf('\0') >= 0) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return normalized;
    }

    private static String digest(String value, String field) {
        String normalized = Objects.requireNonNullElse(value, "").strip().toLowerCase(Locale.ROOT);
        if (!SHA256.matcher(normalized).matches()) {
            throw new IllegalArgumentException(field + " must be a SHA-256 digest");
        }
        return normalized;
    }
}

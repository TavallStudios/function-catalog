package org.tavall.ai.staging;

import java.util.Locale;
import java.util.Objects;

/**
 * Optional Tavall Cloud execution fence carried by repository-staging calls.
 * The standalone Function Catalog provider may receive a {@code null} context,
 * while a Cloud-backed provider must bind every call to one immutable environment.
 */
public record StagingExecutionContext(
        String laneId,
        String environmentId,
        long environmentGeneration,
        long leaseGeneration,
        String sourceSnapshotDigest
) {
    public StagingExecutionContext {
        laneId = normalize(laneId);
        environmentId = normalize(environmentId);
        sourceSnapshotDigest = normalize(sourceSnapshotDigest).toLowerCase(Locale.ROOT);
        if (laneId.isBlank() || environmentId.isBlank()) {
            throw new IllegalArgumentException("laneId and environmentId must be provided");
        }
        if (environmentGeneration <= 0 || leaseGeneration <= 0) {
            throw new IllegalArgumentException("environment and lease generations must be positive");
        }
        if (!sourceSnapshotDigest.matches("[a-f0-9]{64}")) {
            throw new IllegalArgumentException("sourceSnapshotDigest must be a lowercase SHA-256 digest");
        }
    }

    private static String normalize(String value) {
        return Objects.requireNonNullElse(value, "").strip();
    }
}

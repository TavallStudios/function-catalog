package org.tavall.ai.product.intelligence;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;

public record ProductIntelligenceEntry(
        String entryId,
        String productId,
        String agentId,
        String category,
        String key,
        String value,
        String rationale,
        ProductIntelligenceDisposition disposition,
        Set<String> evidenceReferences,
        Instant recordedAt
) {
    public ProductIntelligenceEntry {
        entryId = requireText(entryId, "entryId");
        productId = requireText(productId, "productId");
        agentId = requireText(agentId, "agentId");
        category = requireText(category, "category");
        key = requireText(key, "key");
        value = requireText(value, "value");
        rationale = requireText(rationale, "rationale");
        disposition = Objects.requireNonNull(disposition, "disposition");
        evidenceReferences = Set.copyOf(Objects.requireNonNull(evidenceReferences, "evidenceReferences"));
        recordedAt = Objects.requireNonNull(recordedAt, "recordedAt");
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }
}

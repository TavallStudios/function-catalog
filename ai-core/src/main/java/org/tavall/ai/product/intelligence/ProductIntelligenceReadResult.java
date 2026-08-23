package org.tavall.ai.product.intelligence;

import java.util.List;
import java.util.Objects;

public record ProductIntelligenceReadResult(
        String productId,
        String agentId,
        List<ProductIntelligenceEntry> entries
) {
    public ProductIntelligenceReadResult {
        Objects.requireNonNull(productId, "productId");
        Objects.requireNonNull(agentId, "agentId");
        entries = List.copyOf(Objects.requireNonNull(entries, "entries"));
    }
}

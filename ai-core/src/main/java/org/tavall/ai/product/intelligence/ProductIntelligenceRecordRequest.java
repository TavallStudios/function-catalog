package org.tavall.ai.product.intelligence;

import java.util.List;
import java.util.Objects;

public record ProductIntelligenceRecordRequest(List<ProductIntelligenceEntry> entries) {
    public ProductIntelligenceRecordRequest {
        entries = List.copyOf(Objects.requireNonNull(entries, "entries"));
        if (entries.isEmpty()) {
            throw new IllegalArgumentException("entries must not be empty");
        }
        ProductIntelligenceEntry first = entries.getFirst();
        for (ProductIntelligenceEntry entry : entries) {
            Objects.requireNonNull(entry, "entry");
            if (!first.productId().equals(entry.productId()) || !first.agentId().equals(entry.agentId())) {
                throw new IllegalArgumentException("all entries must share one productId and agentId scope");
            }
        }
    }

    public String productId() { return entries.getFirst().productId(); }
    public String agentId() { return entries.getFirst().agentId(); }
}

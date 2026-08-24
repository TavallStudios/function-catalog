package org.tavall.ai.product.intelligence;

import java.util.List;
import java.util.Objects;

public final class ProductIntelligenceService {
    private final ProductIntelligenceProvider provider;

    public ProductIntelligenceService(ProductIntelligenceProvider provider) {
        this.provider = Objects.requireNonNull(provider, "provider");
    }

    public ProductIntelligenceReadResult read(ProductIntelligenceReadRequest request) {
        ProductIntelligenceReadRequest scoped = Objects.requireNonNull(request, "request");
        List<ProductIntelligenceEntry> entries = List.copyOf(Objects.requireNonNull(
                provider.read(scoped.productId(), scoped.agentId()),
                "provider read result"
        ));
        for (ProductIntelligenceEntry entry : entries) {
            if (!scoped.productId().equals(entry.productId()) || !scoped.agentId().equals(entry.agentId())) {
                throw new IllegalStateException("provider returned product intelligence outside requested scope");
            }
        }
        return new ProductIntelligenceReadResult(scoped.productId(), scoped.agentId(), entries);
    }

    public ProductIntelligenceRecordResult record(ProductIntelligenceRecordRequest request) {
        ProductIntelligenceRecordRequest batch = Objects.requireNonNull(request, "request");
        provider.recordBatch(batch.entries());
        return new ProductIntelligenceRecordResult(batch.productId(), batch.agentId(), batch.entries().size());
    }
}

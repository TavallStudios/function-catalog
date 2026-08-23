package org.tavall.ai.product.intelligence;

import java.util.List;

public interface ProductIntelligenceProvider {
    List<ProductIntelligenceEntry> read(String productId, String agentId);

    /** Persist the complete scoped batch atomically or expose none of it. */
    void recordBatch(List<ProductIntelligenceEntry> entries);
}

package org.tavall.ai.product.intelligence;

public record ProductIntelligenceRecordResult(String productId, String agentId, int recordedCount) {
    public ProductIntelligenceRecordResult {
        if (recordedCount < 1) {
            throw new IllegalArgumentException("recordedCount must be positive");
        }
    }
}

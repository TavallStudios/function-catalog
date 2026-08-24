package org.tavall.ai.product.intelligence;

public record ProductIntelligenceReadRequest(String productId, String agentId) {
    public ProductIntelligenceReadRequest {
        productId = requireText(productId, "productId");
        agentId = requireText(agentId, "agentId");
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }
}

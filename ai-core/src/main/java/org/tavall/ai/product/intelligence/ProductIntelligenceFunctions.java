package org.tavall.ai.product.intelligence;

import org.tavall.ai.core.annotation.AIFunction;
import org.tavall.ai.core.annotation.AIParam;

import java.util.Objects;

public final class ProductIntelligenceFunctions {
    private final ProductIntelligenceService service;

    public ProductIntelligenceFunctions(ProductIntelligenceService service) {
        this.service = Objects.requireNonNull(service, "service");
    }

    @AIFunction(
            name = "product_intelligence_read",
            description = "Read durable product-scoped intelligence for one Tavall agent"
    )
    public ProductIntelligenceReadResult read(
            @AIParam(name = "request") ProductIntelligenceReadRequest request
    ) {
        return service.read(request);
    }

    @AIFunction(
            name = "product_intelligence_record",
            description = "Atomically record one product and agent scoped intelligence batch"
    )
    public ProductIntelligenceRecordResult record(
            @AIParam(name = "request") ProductIntelligenceRecordRequest request
    ) {
        return service.record(request);
    }
}

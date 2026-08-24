package org.tavall.ai.product.intelligence;

import org.tavall.ai.core.catalog.AIFunctionRegistrar;

import java.util.List;
import java.util.Objects;

public final class ProductIntelligenceRegistrar implements AIFunctionRegistrar {
    private final ProductIntelligenceProvider provider;

    public ProductIntelligenceRegistrar(ProductIntelligenceProvider provider) {
        this.provider = Objects.requireNonNull(provider, "provider");
    }

    @Override
    public Iterable<?> instances() {
        return List.of(new ProductIntelligenceFunctions(new ProductIntelligenceService(provider)));
    }
}

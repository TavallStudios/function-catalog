package org.tavall.ai.mcp.server.fixtures;

import org.tavall.ai.core.catalog.AIFunctionRegistrar;

import java.util.List;

public final class NonceRegistrar implements AIFunctionRegistrar {
    @Override
    public Iterable<?> instances() {
        return List.of(new NonceFunctionService(System.getenv("FUNCTION_CATALOG_NONCE")));
    }
}

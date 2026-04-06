package org.tavall.ai.mcp.server.fixtures;

import org.tavall.ai.core.annotation.AIFunction;

public final class NonceFunctionService {
    private final String nonce;

    public NonceFunctionService(String nonce) {
        this.nonce = nonce == null || nonce.isBlank() ? "missing-nonce" : nonce;
    }

    @AIFunction(name = "codex_nonce", description = "Returns the injected nonce for MCP smoke verification")
    public NonceResult nonce() {
        return new NonceResult(nonce);
    }
}

package org.tavall.ai.staging;

import org.tavall.ai.core.catalog.AIFunctionRegistrar;
import org.tavall.ai.staging.github.GitHubRepositoryStagingProvider;

import java.util.List;
import java.util.Objects;

/** Registers the canonical staging function instance against an explicitly scoped repository provider. */
public final class RepositoryStagingRegistrar implements AIFunctionRegistrar {
    private final RepositoryStagingProvider provider;

    /** MCP-friendly constructor. Requires explicit GitHub token + repository allowlist environment configuration. */
    public RepositoryStagingRegistrar() {
        this(GitHubRepositoryStagingProvider.fromEnvironment());
    }

    public RepositoryStagingRegistrar(RepositoryStagingProvider provider) {
        this.provider = Objects.requireNonNull(provider, "provider");
    }

    @Override
    public Iterable<?> instances() {
        return List.of(new RepositoryStagingFunctions(new RepositoryStagingService(provider)));
    }
}

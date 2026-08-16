package org.tavall.ai.review;

import org.tavall.ai.core.catalog.AIFunctionRegistrar;
import org.tavall.ai.review.github.GitHubRepositoryReviewProvider;

import java.util.List;
import java.util.Objects;

/** Registers exact-head GitHub review transport functions for MCP/ChatGPT Web. */
public final class RepositoryReviewRegistrar implements AIFunctionRegistrar {
    private final RepositoryReviewProvider provider;

    public RepositoryReviewRegistrar() {
        this(GitHubRepositoryReviewProvider.fromEnvironment());
    }

    public RepositoryReviewRegistrar(RepositoryReviewProvider provider) {
        this.provider = Objects.requireNonNull(provider, "provider");
    }

    @Override
    public Iterable<?> instances() {
        return List.of(new RepositoryReviewFunctions(new RepositoryReviewService(provider)));
    }
}

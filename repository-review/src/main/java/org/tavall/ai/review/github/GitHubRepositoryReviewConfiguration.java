package org.tavall.ai.review.github;

import org.tavall.ai.review.RepositoryReviewCoordinates;

import java.net.URI;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public record GitHubRepositoryReviewConfiguration(URI apiBase, String token, Set<RepositoryReviewCoordinates> allowedRepositories) {
    public GitHubRepositoryReviewConfiguration {
        if (apiBase == null) throw new IllegalArgumentException("apiBase must not be null");
        if (token == null || token.isBlank()) throw new IllegalArgumentException("GitHub review token must not be blank");
        token = token.trim();
        allowedRepositories = Set.copyOf(allowedRepositories == null ? Set.of() : allowedRepositories);
        if (allowedRepositories.isEmpty()) throw new IllegalArgumentException("At least one GitHub repository must be explicitly allowed");
    }

    public static GitHubRepositoryReviewConfiguration fromEnvironment() {
        String token = System.getenv("FUNCTION_CATALOG_GITHUB_TOKEN");
        String repositories = System.getenv("FUNCTION_CATALOG_GITHUB_REPOSITORIES");
        if (repositories == null || repositories.isBlank()) {
            throw new IllegalStateException("FUNCTION_CATALOG_GITHUB_REPOSITORIES must explicitly list allowed owner/repo values");
        }
        Set<RepositoryReviewCoordinates> allowed = Arrays.stream(repositories.split(","))
                .map(String::trim).filter(value -> !value.isEmpty()).map(RepositoryReviewCoordinates::parse)
                .collect(Collectors.toUnmodifiableSet());
        String api = System.getenv("FUNCTION_CATALOG_GITHUB_API_URL");
        return new GitHubRepositoryReviewConfiguration(
                URI.create(api == null || api.isBlank() ? "https://api.github.com" : api.trim()), token, allowed);
    }
}

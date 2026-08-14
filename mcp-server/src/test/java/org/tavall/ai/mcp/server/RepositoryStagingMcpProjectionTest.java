package org.tavall.ai.mcp.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.tavall.ai.core.catalog.AIFunctionCatalog;
import org.tavall.ai.staging.RepositoryCheck;
import org.tavall.ai.staging.RepositoryCoordinates;
import org.tavall.ai.staging.RepositoryPullRequest;
import org.tavall.ai.staging.RepositoryStagingProvider;
import org.tavall.ai.staging.RepositoryStagingRegistrar;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class RepositoryStagingMcpProjectionTest {
    @Test
    void canonicalJavaFunctionsBecomeMcpToolsWithoutDuplicateSchemas() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        AIFunctionCatalog catalog = new AIFunctionCatalog(objectMapper);
        new RepositoryStagingRegistrar(new EmptyProvider()).register(catalog);

        assertThat(new AIFunctionMcpToolPublisher(objectMapper).toolSpecifications(catalog))
                .extracting(specification -> specification.tool().name())
                .containsExactlyInAnyOrder(
                        "repository_staging_discover",
                        "repository_staging_inspect_graph",
                        "repository_staging_resolve_base",
                        "repository_staging_validate",
                        "repository_staging_ensure",
                        "repository_staging_attach",
                        "repository_staging_set_state",
                        "repository_staging_prepare_promotion"
                );
    }

    private static final class EmptyProvider implements RepositoryStagingProvider {
        @Override
        public List<RepositoryPullRequest> listOpenPullRequests(RepositoryCoordinates repository) {
            return List.of();
        }

        @Override
        public Optional<String> branchHead(RepositoryCoordinates repository, String branch) {
            return Optional.empty();
        }

        @Override
        public void createBranch(RepositoryCoordinates repository, String branch, String sha) {
        }

        @Override
        public RepositoryPullRequest createPullRequest(
                RepositoryCoordinates repository,
                String title,
                String headBranch,
                String baseBranch,
                String body,
                boolean draft
        ) {
            throw new UnsupportedOperationException();
        }

        @Override
        public RepositoryPullRequest updatePullRequest(
                RepositoryCoordinates repository,
                int pullRequestNumber,
                Optional<String> baseBranch,
                Optional<String> body
        ) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<RepositoryCheck> checksForHead(RepositoryCoordinates repository, String headSha) {
            return List.of();
        }
    }
}

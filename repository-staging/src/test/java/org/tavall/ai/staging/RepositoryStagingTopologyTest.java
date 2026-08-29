package org.tavall.ai.staging;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class RepositoryStagingTopologyTest {
    @Test
    void permitsSiblingDomainIntegrationRootsUnderOneRepositoryRoot() {
        RepositoryCoordinates repository = new RepositoryCoordinates("TavallStudios", "example");
        FakeProvider provider = new FakeProvider(List.of(
                pull(10, "combined", "staging/runtime", "main",
                        stagingBody(StagingType.REPOSITORY_INTEGRATION, "staging/runtime", "main")),
                pull(11, "paper", "staging/runtime-paper", "staging/runtime",
                        stagingBody(StagingType.DOMAIN_INTEGRATION, "staging/runtime-paper", "staging/runtime")),
                pull(12, "velocity", "staging/runtime-velocity", "staging/runtime",
                        stagingBody(StagingType.DOMAIN_INTEGRATION, "staging/runtime-velocity", "staging/runtime")),
                pull(13, "discord", "staging/runtime-discord", "staging/runtime",
                        stagingBody(StagingType.DOMAIN_INTEGRATION, "staging/runtime-discord", "staging/runtime"))
        ));

        StagingValidationResult result = new RepositoryStagingService(provider)
                .validate(new RepositoryStagingRequest(repository));

        assertThat(result.valid()).isTrue();
        assertThat(result.findings())
                .noneMatch(finding -> finding.code().equals("DUPLICATE_ACTIVE_STAGING_ROOT"));
    }

    @Test
    void permitsSiblingPaperOwnedDomainsUnderPaperRoot() {
        RepositoryCoordinates repository = new RepositoryCoordinates("TavallStudios", "example");
        FakeProvider provider = new FakeProvider(List.of(
                pull(10, "combined", "staging/runtime", "main",
                        stagingBody(StagingType.REPOSITORY_INTEGRATION, "staging/runtime", "main")),
                pull(11, "paper", "staging/runtime-paper", "staging/runtime",
                        stagingBody(StagingType.DOMAIN_INTEGRATION, "staging/runtime-paper", "staging/runtime")),
                pull(12, "ffa", "staging/runtime-ffa", "staging/runtime-paper",
                        stagingBody(StagingType.DOMAIN_INTEGRATION, "staging/runtime-ffa", "staging/runtime-paper")),
                pull(13, "kingdoms", "staging/runtime-kingdoms", "staging/runtime-paper",
                        stagingBody(StagingType.DOMAIN_INTEGRATION, "staging/runtime-kingdoms", "staging/runtime-paper"))
        ));

        StagingValidationResult result = new RepositoryStagingService(provider)
                .validate(new RepositoryStagingRequest(repository));

        assertThat(result.valid()).isTrue();
        assertThat(result.findings())
                .noneMatch(finding -> finding.code().equals("DUPLICATE_ACTIVE_STAGING_ROOT"));
    }

    @Test
    void stillRejectsDuplicateActiveDomainIdentity() {
        RepositoryCoordinates repository = new RepositoryCoordinates("TavallStudios", "example");
        FakeProvider provider = new FakeProvider(List.of(
                pull(11, "paper one", "staging/runtime-paper", "staging/runtime",
                        stagingBody(StagingType.DOMAIN_INTEGRATION, "staging/runtime-paper", "staging/runtime")),
                pull(12, "paper duplicate", "staging/runtime-paper", "staging/other-parent",
                        stagingBody(StagingType.DOMAIN_INTEGRATION, "staging/runtime-paper", "staging/other-parent"))
        ));

        StagingValidationResult result = new RepositoryStagingService(provider)
                .validate(new RepositoryStagingRequest(repository));

        assertThat(result.valid()).isFalse();
        assertThat(result.findings())
                .filteredOn(finding -> finding.code().equals("DUPLICATE_ACTIVE_STAGING_ROOT"))
                .hasSize(2)
                .allMatch(finding -> finding.message().contains("DOMAIN_INTEGRATION|staging/runtime-paper"));
    }

    @Test
    void stillRejectsDuplicateRepositoryIntegrationRootsForOneParent() {
        RepositoryCoordinates repository = new RepositoryCoordinates("TavallStudios", "example");
        FakeProvider provider = new FakeProvider(List.of(
                pull(10, "combined one", "staging/runtime", "main",
                        stagingBody(StagingType.REPOSITORY_INTEGRATION, "staging/runtime", "main")),
                pull(11, "combined duplicate", "staging/runtime-alt", "main",
                        stagingBody(StagingType.REPOSITORY_INTEGRATION, "staging/runtime-alt", "main"))
        ));

        StagingValidationResult result = new RepositoryStagingService(provider)
                .validate(new RepositoryStagingRequest(repository));

        assertThat(result.valid()).isFalse();
        assertThat(result.findings())
                .filteredOn(finding -> finding.code().equals("DUPLICATE_ACTIVE_STAGING_ROOT"))
                .hasSize(2);
    }

    private static RepositoryPullRequest pull(
            int number,
            String title,
            String head,
            String base,
            String body
    ) {
        return new RepositoryPullRequest(number, title, body, head, head + "-sha", base, base + "-sha", true);
    }

    private static String stagingBody(StagingType type, String branch, String parent) {
        return "<!-- tavall-staging:v1 -->\n"
                + "Type: " + type + "\n"
                + "State: ACTIVE\n"
                + "Branch: " + branch + "\n"
                + "Parent: " + parent + "\n"
                + "Promotion: MANUAL\n"
                + "ChildMergeTarget: " + branch + "\n";
    }

    private static final class FakeProvider implements RepositoryStagingProvider {
        private final Map<Integer, RepositoryPullRequest> pulls = new LinkedHashMap<>();

        private FakeProvider(List<RepositoryPullRequest> pulls) {
            pulls.forEach(pull -> this.pulls.put(pull.number(), pull));
        }

        @Override
        public List<RepositoryPullRequest> listOpenPullRequests(RepositoryCoordinates repository) {
            return List.copyOf(pulls.values());
        }

        @Override
        public Optional<String> branchHead(RepositoryCoordinates repository, String branch) {
            return Optional.of(branch + "-sha");
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

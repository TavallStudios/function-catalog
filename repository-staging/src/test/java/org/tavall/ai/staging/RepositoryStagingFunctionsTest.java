package org.tavall.ai.staging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.tavall.ai.core.catalog.AIFunctionCatalog;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class RepositoryStagingFunctionsTest {
    @Test
    void registrarPublishesCanonicalRepositoryStagingFunctions() {
        AIFunctionCatalog catalog = new AIFunctionCatalog(new ObjectMapper().findAndRegisterModules());
        RepositoryStagingProvider provider = new FakeProvider();
        new RepositoryStagingRegistrar(provider).register(catalog);

        assertThat(catalog.getFunctionDefinitions().keySet()).containsExactlyInAnyOrder(
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

    @Test
    void metadataRoundTripsAndPreservesNonMetadataBody() {
        String original = "Intro\n\n<!-- tavall-staging:v1 -->\n"
                + "Type: REPOSITORY_INTEGRATION\n"
                + "State: ACTIVE\n"
                + "Branch: staging/runtime\n"
                + "Parent: main\n"
                + "Promotion: MANUAL\n"
                + "ChildMergeTarget: staging/runtime\n\n"
                + "Details after metadata.\n";

        StagingMetadataDocument parsed = StagingMetadataDocument.parse(original);

        assertThat(parsed.metadata()).contains(new StagingMetadata(
                StagingType.REPOSITORY_INTEGRATION,
                StagingState.ACTIVE,
                "staging/runtime",
                "main",
                "MANUAL",
                "staging/runtime"
        ));
        assertThat(parsed.withState(StagingState.FROZEN)).contains("State: FROZEN");
        assertThat(parsed.withState(StagingState.FROZEN)).contains("Intro");
        assertThat(parsed.withState(StagingState.FROZEN)).contains("Details after metadata.");
    }

    @Test
    void resolveBasePreservesExistingFeatureStackBeforeChoosingStaging() {
        RepositoryCoordinates repository = new RepositoryCoordinates("TavallStudios", "example");
        FakeProvider provider = new FakeProvider(List.of(
                pull(10, "Runtime: Staging PR — Example", "staging/runtime", "main", stagingBody()),
                pull(20, "feature parent", "working/parent", "staging/runtime", "feature"),
                pull(21, "feature child", "working/child", "working/parent", "feature")
        ));
        RepositoryStagingService service = new RepositoryStagingService(provider);

        StagingBaseResolution resolution = service.resolveBase(new ResolveStagingBaseRequest(
                repository,
                21,
                null,
                null,
                null
        ));

        assertThat(resolution.resolved()).isTrue();
        assertThat(resolution.baseBranch()).isEqualTo("working/parent");
        assertThat(resolution.parentPullRequestNumber()).isEqualTo(20);
        assertThat(resolution.reason()).isEqualTo(StagingBaseReason.PRESERVE_EXISTING_FEATURE_PARENT);
    }

    @Test
    void validateWarnsAboutDirectToMainWorkWithoutMakingIntentionalHotfixTopologyInvalid() {
        RepositoryCoordinates repository = new RepositoryCoordinates("TavallStudios", "example");
        FakeProvider provider = new FakeProvider(List.of(
                pull(10, "Runtime: Staging PR — Example", "staging/runtime", "main", stagingBody()),
                pull(30, "possible hotfix", "working/hotfix", "main", "feature")
        ));

        StagingValidationResult result = new RepositoryStagingService(provider)
                .validate(new RepositoryStagingRequest(repository));

        assertThat(result.valid()).isTrue();
        assertThat(result.findings())
                .filteredOn(finding -> finding.code().equals("DIRECT_TO_MAIN_WITH_ACTIVE_STAGING"))
                .singleElement()
                .extracting(StagingTopologyFinding::severity)
                .isEqualTo(StagingFindingSeverity.WARNING);
    }

    @Test
    void attachMovesOnlyIndependentOrStackRootWithoutFlatteningChildren() {
        RepositoryCoordinates repository = new RepositoryCoordinates("TavallStudios", "example");
        FakeProvider provider = new FakeProvider(List.of(
                pull(10, "Runtime: Staging PR — Example", "staging/runtime", "main", stagingBody()),
                pull(40, "stack root", "working/root", "main", "feature"),
                pull(41, "child", "working/child", "working/root", "feature")
        ));
        RepositoryStagingService service = new RepositoryStagingService(provider);

        StagingAttachResult result = service.attach(new AttachStagingPullRequestRequest(repository, 40, 10));

        assertThat(result.attached()).isTrue();
        assertThat(provider.pullRequests.get(40).baseBranch()).isEqualTo("staging/runtime");
        assertThat(provider.pullRequests.get(41).baseBranch()).isEqualTo("working/root");
    }

    @Test
    void promotionPreparationBlocksOnOpenChildrenAndNonSuccessfulChecks() {
        RepositoryCoordinates repository = new RepositoryCoordinates("TavallStudios", "example");
        FakeProvider provider = new FakeProvider(List.of(
                pull(10, "Runtime: Staging PR — Example", "staging/runtime", "main", frozenStagingBody()),
                pull(50, "still integrating", "working/child", "staging/runtime", "feature")
        ));
        provider.checks = List.of(new RepositoryCheck("local-ci", RepositoryCheckStatus.COMPLETED, "FAILURE"));

        StagingPromotionPreparation result = new RepositoryStagingService(provider)
                .preparePromotion(new PrepareStagingPromotionRequest(repository, 10));

        assertThat(result.blocked()).isTrue();
        assertThat(result.openChildPullRequests()).containsExactly(50);
        assertThat(result.blockers()).anyMatch(value -> value.contains("local-ci"));
    }

    private static RepositoryPullRequest pull(int number, String title, String head, String base, String body) {
        return new RepositoryPullRequest(number, title, body, head, head + "-sha", base, base + "-sha", true);
    }

    private static String stagingBody() {
        return "<!-- tavall-staging:v1 -->\nType: REPOSITORY_INTEGRATION\nState: ACTIVE\nBranch: staging/runtime\nParent: main\nPromotion: MANUAL\nChildMergeTarget: staging/runtime\n";
    }

    private static String frozenStagingBody() {
        return stagingBody().replace("State: ACTIVE", "State: FROZEN");
    }

    private static final class FakeProvider implements RepositoryStagingProvider {
        private final Map<Integer, RepositoryPullRequest> pullRequests = new java.util.LinkedHashMap<>();
        private List<RepositoryCheck> checks = List.of();

        private FakeProvider() {
        }

        private FakeProvider(List<RepositoryPullRequest> pullRequests) {
            pullRequests.forEach(pull -> this.pullRequests.put(pull.number(), pull));
        }

        @Override
        public List<RepositoryPullRequest> listOpenPullRequests(RepositoryCoordinates repository) {
            return List.copyOf(pullRequests.values());
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
            RepositoryPullRequest created = pull(99, title, headBranch, baseBranch, body);
            pullRequests.put(created.number(), created);
            return created;
        }

        @Override
        public RepositoryPullRequest updatePullRequest(
                RepositoryCoordinates repository,
                int pullRequestNumber,
                Optional<String> baseBranch,
                Optional<String> body
        ) {
            RepositoryPullRequest old = pullRequests.get(pullRequestNumber);
            RepositoryPullRequest updated = new RepositoryPullRequest(
                    old.number(),
                    old.title(),
                    body.orElse(old.body()),
                    old.headBranch(),
                    old.headSha(),
                    baseBranch.orElse(old.baseBranch()),
                    old.baseSha(),
                    old.draft()
            );
            pullRequests.put(updated.number(), updated);
            return updated;
        }

        @Override
        public List<RepositoryCheck> checksForHead(RepositoryCoordinates repository, String headSha) {
            return checks;
        }
    }
}

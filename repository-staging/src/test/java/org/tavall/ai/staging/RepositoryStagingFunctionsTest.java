package org.tavall.ai.staging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.tavall.ai.core.catalog.AIFunctionCatalog;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RepositoryStagingFunctionsTest {
    @Test
    void creationValidatesAncestryBeforeAnyProviderWrite() {
        var repository = new RepositoryCoordinates("TavallStudios", "example");
        var provider = new FakeProvider(List.of(pull(10, "staging", "staging/runtime", "main", stagingBody())));
        provider.mutationHead = "a".repeat(40);
        var service = new RepositoryStagingService(provider);
        assertThatThrownBy(() -> service.createPullRequest(new CreateStagedPullRequestRequest(
                repository, "feature", "body", "working/feature", provider.mutationHead, "main", true)))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("Invalid staging topology");
        assertThat(provider.writes).isZero();
        assertThatThrownBy(() -> service.createPullRequest(new CreateStagedPullRequestRequest(
                repository, "feature", "body", "working/feature", "b".repeat(40), "staging/runtime", true)))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("STALE_SOURCE");
        assertThat(provider.writes).isZero();
        var created = service.createPullRequest(new CreateStagedPullRequestRequest(
                repository, "feature", "body", "working/feature", provider.mutationHead, "staging/runtime", true));
        assertThat(created.baseBranch()).isEqualTo("staging/runtime");
        assertThat(provider.writes).isEqualTo(1);
        assertThat(service.validate(new RepositoryStagingRequest(repository)).valid()).isTrue();
    }

    @Test
    void updateCannotRemoveStagingMetadataOrFlattenAnOpenDependency() {
        var repository = new RepositoryCoordinates("TavallStudios", "example");
        String head = "a".repeat(40);
        var provider = new FakeProvider(List.of(
                new RepositoryPullRequest(10, "staging", stagingBody(), "staging/runtime", head, "main", head, true),
                new RepositoryPullRequest(20, "parent", "", "working/parent", head, "staging/runtime", head, true),
                new RepositoryPullRequest(21, "child", "", "working/child", head, "working/parent", head, true)));
        var service = new RepositoryStagingService(provider);
        assertThatThrownBy(() -> service.updatePullRequest(new UpdateStagedPullRequestRequest(
                repository, 10, head, Optional.empty(), Optional.of("removed metadata"))))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("Invalid staging topology");
        assertThatThrownBy(() -> service.updatePullRequest(new UpdateStagedPullRequestRequest(
                repository, 21, head, Optional.of("staging/runtime"), Optional.empty())))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("Preserve the open feature dependency");
        assertThat(provider.writes).isZero();
        var updated = service.updatePullRequest(new UpdateStagedPullRequestRequest(
                repository, 21, head, Optional.empty(), Optional.of("updated description")));
        assertThat(updated.body()).isEqualTo("updated description");
        assertThat(updated.baseBranch()).isEqualTo("working/parent");
    }

    @Test
    void postPublicationDriftReturnsAnExplicitRecoveryStateWithTheCreatedPr() {
        var repository = new RepositoryCoordinates("TavallStudios", "example");
        var provider = new FakeProvider(List.of(pull(10, "staging", "staging/runtime", "main", stagingBody())));
        provider.mutationHead = "a".repeat(40);
        provider.driftAfterCreate = true;
        assertThatThrownBy(() -> new RepositoryStagingService(provider).createPullRequest(
                new CreateStagedPullRequestRequest(repository, "feature", "body", "working/feature",
                        provider.mutationHead, "staging/runtime", true)))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("RECOVERY_REQUIRED: published PR #99");
        assertThat(provider.writes).isEqualTo(1);
    }

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
    void metadataParserAcceptsPersistentRuntimeFieldsAndPreservesThemOnStateChanges() {
        String original = "Intro\n\n<!-- tavall-staging:v1 -->\n"
                + "Type: COMBINED_RUNTIME_INTEGRATION\n"
                + "Lifecycle: PERSISTENT\n"
                + "State: ACTIVE\n"
                + "Branch: stabilize/full-runtime-build\n"
                + "Parent: main\n"
                + "Promotion: MANUAL\n"
                + "ChildMergeTarget: stabilize/full-runtime-build\n"
                + "RuntimeId: NONE\n"
                + "RuntimeStack: tavall-project-novus\n"
                + "FanInMode: SNAPSHOT_NON_CLOSING\n"
                + "RuntimeFlags: PAPER=ENABLED;WEB=MIGRATING_TO_TAVALL_WEB\n"
                + "ArchitectureProfile: architecture-combined\n"
                + "ArchitectureCheck: tavall-ci/manual/architecture-combined\n\n"
                + "Details after metadata.\n";

        StagingMetadataDocument parsed = StagingMetadataDocument.parse(original);

        assertThat(parsed.metadata()).isPresent();
        assertThat(parsed.metadata().orElseThrow().lifecycle()).contains("PERSISTENT");
        assertThat(parsed.metadata().orElseThrow().runtimeStack()).contains("tavall-project-novus");
        assertThat(parsed.metadata().orElseThrow().fanInMode()).contains("SNAPSHOT_NON_CLOSING");
        assertThat(parsed.withState(StagingState.FROZEN))
                .contains("Lifecycle: PERSISTENT")
                .contains("RuntimeFlags: PAPER=ENABLED;WEB=MIGRATING_TO_TAVALL_WEB")
                .contains("ArchitectureCheck: tavall-ci/manual/architecture-combined")
                .contains("Details after metadata.");
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
    void validateRejectsDirectToMainWorkWithoutStagingAncestry() {
        RepositoryCoordinates repository = new RepositoryCoordinates("TavallStudios", "example");
        FakeProvider provider = new FakeProvider(List.of(
                pull(10, "Runtime: Staging PR — Example", "staging/runtime", "main", stagingBody()),
                pull(30, "possible hotfix", "working/hotfix", "main", "feature")
        ));

        StagingValidationResult result = new RepositoryStagingService(provider)
                .validate(new RepositoryStagingRequest(repository));

        assertThat(result.valid()).isFalse();
        assertThat(result.findings())
                .filteredOn(finding -> finding.code().equals("DIRECT_TO_MAIN_WITH_ACTIVE_STAGING"))
                .singleElement()
                .extracting(StagingTopologyFinding::severity)
                .isEqualTo(StagingFindingSeverity.ERROR);
    }

    @Test
    void validateRejectsAStackWhoseRootHasNoStagingPath() {
        RepositoryCoordinates repository = new RepositoryCoordinates("TavallStudios", "example");
        FakeProvider provider = new FakeProvider(List.of(
                pull(40, "stack root", "working/root", "main", "feature"),
                pull(41, "stack child", "working/child", "working/root", "feature")
        ));

        StagingValidationResult result = new RepositoryStagingService(provider)
                .validate(new RepositoryStagingRequest(repository));

        assertThat(result.valid()).isFalse();
        assertThat(result.findings())
                .filteredOn(finding -> finding.code().equals("PR_WITHOUT_STAGING_ANCESTRY"))
                .extracting(StagingTopologyFinding::pullRequestNumber)
                .containsExactlyInAnyOrder(40, 41);
    }

    @Test
    void validateRejectsAnOrphanSubStagingPullRequest() {
        RepositoryCoordinates repository = new RepositoryCoordinates("TavallStudios", "example");
        FakeProvider provider = new FakeProvider(List.of(
                pull(70, "Domain staging", "staging/domain", "main", domainStagingBody())
        ));

        StagingValidationResult result = new RepositoryStagingService(provider)
                .validate(new RepositoryStagingRequest(repository));

        assertThat(result.valid()).isFalse();
        assertThat(result.findings())
                .filteredOn(finding -> finding.code().equals("SUB_STAGING_WITHOUT_ACTIVE_PARENT"))
                .singleElement()
                .extracting(StagingTopologyFinding::severity)
                .isEqualTo(StagingFindingSeverity.ERROR);
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
    void attachRejectsFrozenTargets() {
        RepositoryCoordinates repository = new RepositoryCoordinates("TavallStudios", "example");
        FakeProvider provider = new FakeProvider(List.of(
                pull(10, "Frozen staging", "staging/runtime", "main", frozenStagingBody()),
                pull(40, "stack root", "working/root", "main", "feature")
        ));

        assertThatThrownBy(() -> new RepositoryStagingService(provider)
                .attach(new AttachStagingPullRequestRequest(repository, 40, 10)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ACTIVE");
    }

    @Test
    void attachRewritesStagingMetadataParentAlongsidePullRequestBase() {
        RepositoryCoordinates repository = new RepositoryCoordinates("TavallStudios", "example");
        FakeProvider provider = new FakeProvider(List.of(
                pull(10, "Runtime staging", "staging/runtime", "main", stagingBody()),
                pull(40, "Domain staging", "staging/domain", "main", domainStagingBody())
        ));

        StagingAttachResult result = new RepositoryStagingService(provider)
                .attach(new AttachStagingPullRequestRequest(repository, 40, 10));

        assertThat(result.attached()).isTrue();
        assertThat(provider.pullRequests.get(40).baseBranch()).isEqualTo("staging/runtime");
        assertThat(provider.pullRequests.get(40).body()).contains("Parent: staging/runtime");
    }

    @Test
    void supersedingAStagingPullRequestWithOpenDescendantsIsRejected() {
        RepositoryCoordinates repository = new RepositoryCoordinates("TavallStudios", "example");
        FakeProvider provider = new FakeProvider(List.of(
                pull(10, "Runtime staging", "staging/runtime", "main", stagingBody()),
                pull(40, "attached root", "working/root", "staging/runtime", "feature")
        ));

        assertThatThrownBy(() -> new RepositoryStagingService(provider)
                .setState(new SetStagingStateRequest(repository, 10, StagingState.SUPERSEDED)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("open descendants");
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

    private static String domainStagingBody() {
        return "<!-- tavall-staging:v1 -->\nType: DOMAIN_INTEGRATION\nState: ACTIVE\n"
                + "Branch: staging/domain\nParent: main\nPromotion: MANUAL\nChildMergeTarget: staging/domain\n";
    }

    private static final class FakeProvider implements RepositoryStagingProvider {
        private final Map<Integer, RepositoryPullRequest> pullRequests = new java.util.LinkedHashMap<>();
        private List<RepositoryCheck> checks = List.of();
        private String mutationHead;
        private int writes;
        private boolean driftAfterCreate;

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
            return Optional.of(mutationHead == null ? branch + "-sha" : mutationHead);
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
            writes++;
            if (mutationHead != null) {
                created = new RepositoryPullRequest(99, title, body, headBranch, mutationHead,
                        driftAfterCreate ? "main" : baseBranch, mutationHead, draft);
            }
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
            writes++;
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

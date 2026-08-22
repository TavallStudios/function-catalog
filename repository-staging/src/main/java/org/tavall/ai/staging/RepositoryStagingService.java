package org.tavall.ai.staging;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Provider-neutral Tavall staging graph semantics. */
public final class RepositoryStagingService {
    private final RepositoryStagingProvider provider;

    public RepositoryStagingService(RepositoryStagingProvider provider) {
        this.provider = Objects.requireNonNull(provider, "provider");
    }

    /** Returns a semantic service view bound to the supplied execution fence. */
    public RepositoryStagingService scoped(StagingExecutionContext context) {
        return new RepositoryStagingService(provider.scoped(context));
    }

    public StagingDiscoveryResult discover(RepositoryStagingRequest request) {
        StagingGraph graph = inspectGraph(request);
        return new StagingDiscoveryResult(graph.stagingPullRequests(), graph.findings());
    }

    public StagingGraph inspectGraph(RepositoryStagingRequest request) {
        List<RepositoryPullRequest> pulls = provider.listOpenPullRequests(request.repository());
        Map<String, RepositoryPullRequest> byHead = pulls.stream().collect(Collectors.toMap(
                RepositoryPullRequest::headBranch,
                Function.identity(),
                (left, right) -> left,
                LinkedHashMap::new
        ));
        List<StagingPullRequest> staging = new ArrayList<>();
        List<StagingTopologyFinding> findings = new ArrayList<>();
        List<StagingRelationship> relationships = new ArrayList<>();

        for (RepositoryPullRequest pull : pulls) {
            StagingMetadataDocument document = StagingMetadataDocument.parse(pull.body());
            if (document.malformed()) {
                findings.add(finding(
                        "MALFORMED_STAGING_METADATA",
                        StagingFindingSeverity.ERROR,
                        "Pull request contains malformed tavall-staging:v1 metadata",
                        pull.number()
                ));
            }
            document.metadata().ifPresent(metadata -> {
                staging.add(new StagingPullRequest(pull, metadata));
                if (!metadata.branch().equals(pull.headBranch())) {
                    findings.add(finding(
                            "STAGING_BRANCH_MISMATCH",
                            StagingFindingSeverity.ERROR,
                            "Metadata Branch does not match pull request head branch",
                            pull.number()
                    ));
                }
                if (!metadata.parent().equals(pull.baseBranch())) {
                    findings.add(finding(
                            "STAGING_PARENT_MISMATCH",
                            StagingFindingSeverity.ERROR,
                            "Metadata Parent does not match pull request base branch",
                            pull.number()
                    ));
                }
                if (!metadata.childMergeTarget().equals(metadata.branch())) {
                    findings.add(finding(
                            "STAGING_CHILD_TARGET_MISMATCH",
                            StagingFindingSeverity.ERROR,
                            "ChildMergeTarget must match the staging branch",
                            pull.number()
                    ));
                }
            });

            RepositoryPullRequest parent = byHead.get(pull.baseBranch());
            if (parent != null) {
                relationships.add(new StagingRelationship(parent.number(), pull.number()));
            }
        }

        Map<String, List<StagingPullRequest>> activeGroups = staging.stream()
                .filter(value -> value.metadata().state() == StagingState.ACTIVE)
                .collect(Collectors.groupingBy(value -> value.metadata().type() + "|" + value.metadata().parent()));
        for (Map.Entry<String, List<StagingPullRequest>> entry : activeGroups.entrySet()) {
            if (entry.getValue().size() > 1) {
                for (StagingPullRequest duplicate : entry.getValue()) {
                    findings.add(finding(
                            "DUPLICATE_ACTIVE_STAGING_ROOT",
                            StagingFindingSeverity.ERROR,
                            "Multiple active staging roots share type/parent " + entry.getKey(),
                            duplicate.pullRequest().number()
                    ));
                }
            }
        }

        boolean activeRepositoryStaging = staging.stream().anyMatch(value ->
                value.metadata().type() == StagingType.REPOSITORY_INTEGRATION
                        && value.metadata().state() == StagingState.ACTIVE);
        Set<Integer> stagingNumbers = staging.stream()
                .map(value -> value.pullRequest().number())
                .collect(Collectors.toSet());
        if (activeRepositoryStaging) {
            for (RepositoryPullRequest pull : pulls) {
                if (!stagingNumbers.contains(pull.number()) && "main".equals(pull.baseBranch())) {
                    findings.add(finding(
                            "DIRECT_TO_MAIN_WITH_ACTIVE_STAGING",
                            StagingFindingSeverity.WARNING,
                            "Non-staging pull request targets main while active repository staging exists; confirm intentional hotfix or reattach/reconcile it into staging",
                            pull.number()
                    ));
                }
            }
        }

        detectCycles(relationships, findings);
        return new StagingGraph(pulls, staging, relationships, findings);
    }

    public StagingBaseResolution resolveBase(ResolveStagingBaseRequest request) {
        StagingGraph graph = inspectGraph(new RepositoryStagingRequest(request.repository()));
        Map<Integer, RepositoryPullRequest> pulls = graph.pullRequests().stream()
                .collect(Collectors.toMap(RepositoryPullRequest::number, Function.identity()));
        Map<String, RepositoryPullRequest> byHead = graph.pullRequests().stream()
                .collect(Collectors.toMap(
                        RepositoryPullRequest::headBranch,
                        Function.identity(),
                        (left, right) -> left
                ));
        Set<Integer> stagingNumbers = graph.stagingPullRequests().stream()
                .map(value -> value.pullRequest().number())
                .collect(Collectors.toSet());

        if (request.dependentOnPullRequestNumber() != null) {
            RepositoryPullRequest parent = requirePull(pulls, request.dependentOnPullRequestNumber());
            return resolved(parent, StagingBaseReason.EXPLICIT_DEPENDENCY, graph.findings());
        }

        if (request.currentPullRequestNumber() != null) {
            RepositoryPullRequest current = requirePull(pulls, request.currentPullRequestNumber());
            RepositoryPullRequest parent = byHead.get(current.baseBranch());
            if (parent != null) {
                return resolved(
                        parent,
                        stagingNumbers.contains(parent.number())
                                ? StagingBaseReason.PRESERVE_EXISTING_STAGING_PARENT
                                : StagingBaseReason.PRESERVE_EXISTING_FEATURE_PARENT,
                        graph.findings()
                );
            }
        }

        List<StagingPullRequest> candidates = graph.stagingPullRequests().stream()
                .filter(value -> value.metadata().state() == StagingState.ACTIVE)
                .toList();
        if (request.preferredStagingBranch() != null) {
            List<StagingPullRequest> exact = candidates.stream()
                    .filter(value -> request.preferredStagingBranch().equals(value.metadata().branch()))
                    .toList();
            if (exact.size() == 1) {
                return resolved(exact.getFirst().pullRequest(), StagingBaseReason.EXACT_STAGING_BRANCH, graph.findings());
            }
            candidates = exact;
        } else if (request.preferredStagingType() != null) {
            candidates = candidates.stream()
                    .filter(value -> value.metadata().type() == request.preferredStagingType())
                    .toList();
            if (candidates.size() == 1) {
                return resolved(
                        candidates.getFirst().pullRequest(),
                        StagingBaseReason.UNIQUE_STAGING_TYPE,
                        graph.findings()
                );
            }
        } else {
            List<StagingPullRequest> repositoryRoots = candidates.stream()
                    .filter(value -> value.metadata().type() == StagingType.REPOSITORY_INTEGRATION)
                    .toList();
            if (repositoryRoots.size() == 1) {
                return resolved(
                        repositoryRoots.getFirst().pullRequest(),
                        StagingBaseReason.UNIQUE_REPOSITORY_STAGING,
                        graph.findings()
                );
            }
            candidates = repositoryRoots;
        }

        List<Integer> numbers = candidates.stream()
                .map(value -> value.pullRequest().number())
                .toList();
        return new StagingBaseResolution(
                false,
                "",
                null,
                numbers.isEmpty() ? StagingBaseReason.NO_STAGING_FOUND : StagingBaseReason.AMBIGUOUS_STAGING,
                numbers,
                graph.findings()
        );
    }

    public StagingValidationResult validate(RepositoryStagingRequest request) {
        List<StagingTopologyFinding> findings = inspectGraph(request).findings();
        boolean valid = findings.stream().noneMatch(value -> value.severity() == StagingFindingSeverity.ERROR);
        return new StagingValidationResult(valid, findings);
    }

    public StagingEnsureResult ensure(EnsureStagingPullRequestRequest request) {
        StagingGraph graph = inspectGraph(new RepositoryStagingRequest(request.repository()));
        Optional<StagingPullRequest> existing = graph.stagingPullRequests().stream()
                .filter(value -> value.metadata().state() != StagingState.SUPERSEDED)
                .filter(value -> value.metadata().type() == request.type())
                .filter(value -> value.metadata().branch().equals(request.branch()))
                .findFirst();
        if (existing.isPresent()) {
            return new StagingEnsureResult(false, existing.get());
        }

        String parentSha = provider.branchHead(request.repository(), request.parentBranch())
                .orElseThrow(() -> new IllegalStateException(
                        "Parent branch does not exist: " + request.parentBranch()
                ));
        if (provider.branchHead(request.repository(), request.branch()).isEmpty()) {
            provider.createBranch(request.repository(), request.branch(), parentSha);
        }

        StagingMetadata metadata = new StagingMetadata(
                request.type(),
                StagingState.ACTIVE,
                request.branch(),
                request.parentBranch(),
                "MANUAL",
                request.branch()
        );
        RepositoryPullRequest created = provider.createPullRequest(
                request.repository(),
                request.title(),
                request.branch(),
                request.parentBranch(),
                StagingMetadataDocument.render(metadata),
                true
        );
        return new StagingEnsureResult(true, new StagingPullRequest(created, metadata));
    }

    public StagingAttachResult attach(AttachStagingPullRequestRequest request) {
        StagingGraph graph = inspectGraph(new RepositoryStagingRequest(request.repository()));
        Map<Integer, RepositoryPullRequest> pulls = graph.pullRequests().stream()
                .collect(Collectors.toMap(RepositoryPullRequest::number, Function.identity()));
        Map<String, RepositoryPullRequest> byHead = graph.pullRequests().stream()
                .collect(Collectors.toMap(
                        RepositoryPullRequest::headBranch,
                        Function.identity(),
                        (left, right) -> left
                ));

        RepositoryPullRequest source = requirePull(pulls, request.pullRequestNumber());
        StagingPullRequest target = graph.stagingPullRequests().stream()
                .filter(value -> value.pullRequest().number() == request.stagingPullRequestNumber())
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Target is not a staging pull request"));
        if (target.metadata().state() == StagingState.SUPERSEDED) {
            throw new IllegalStateException("Cannot attach to a superseded staging pull request");
        }

        RepositoryPullRequest currentParent = byHead.get(source.baseBranch());
        if (currentParent != null) {
            boolean currentParentIsStaging = graph.stagingPullRequests().stream()
                    .anyMatch(value -> value.pullRequest().number() == currentParent.number());
            if (!currentParentIsStaging) {
                throw new IllegalStateException(
                        "Only an independent pull request or stack root may be reparented; preserve its feature parent"
                );
            }
        }

        provider.updatePullRequest(
                request.repository(),
                source.number(),
                Optional.of(target.metadata().branch()),
                Optional.empty()
        );
        return new StagingAttachResult(
                true,
                source.number(),
                target.pullRequest().number(),
                target.metadata().branch()
        );
    }

    public StagingStateResult setState(SetStagingStateRequest request) {
        StagingGraph graph = inspectGraph(new RepositoryStagingRequest(request.repository()));
        StagingPullRequest staging = graph.stagingPullRequests().stream()
                .filter(value -> value.pullRequest().number() == request.stagingPullRequestNumber())
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Pull request is not a valid staging pull request"
                ));
        StagingState previous = staging.metadata().state();
        if (previous != request.state() && !allowedTransitions(previous).contains(request.state())) {
            throw new IllegalStateException(
                    "Invalid staging state transition: " + previous + " -> " + request.state()
            );
        }

        String body = StagingMetadataDocument.parse(staging.pullRequest().body()).withState(request.state());
        provider.updatePullRequest(
                request.repository(),
                staging.pullRequest().number(),
                Optional.empty(),
                Optional.of(body)
        );
        return new StagingStateResult(staging.pullRequest().number(), previous, request.state());
    }

    public StagingPromotionPreparation preparePromotion(PrepareStagingPromotionRequest request) {
        StagingGraph graph = inspectGraph(new RepositoryStagingRequest(request.repository()));
        StagingPullRequest staging = graph.stagingPullRequests().stream()
                .filter(value -> value.pullRequest().number() == request.stagingPullRequestNumber())
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Pull request is not a valid staging pull request"
                ));

        List<Integer> children = graph.pullRequests().stream()
                .filter(value -> value.baseBranch().equals(staging.pullRequest().headBranch()))
                .map(RepositoryPullRequest::number)
                .sorted()
                .toList();
        List<RepositoryCheck> checks = provider.checksForHead(
                request.repository(),
                staging.pullRequest().headSha()
        );
        List<String> blockers = new ArrayList<>();
        if (staging.metadata().state() != StagingState.FROZEN
                && staging.metadata().state() != StagingState.PROMOTING) {
            blockers.add("Staging scope must be FROZEN or PROMOTING before promotion preparation");
        }
        if (!children.isEmpty()) {
            blockers.add("Open child pull requests remain: " + children);
        }
        if (checks.isEmpty()) {
            blockers.add("No validation checks are recorded for exact staging head " + staging.pullRequest().headSha());
        }
        for (RepositoryCheck check : checks) {
            if (check.status() != RepositoryCheckStatus.COMPLETED) {
                blockers.add("Check is not complete: " + check.name());
            } else if (!successfulConclusion(check.conclusion())) {
                blockers.add("Check is not successful: " + check.name() + " (" + check.conclusion() + ")");
            }
        }
        graph.findings().stream()
                .filter(value -> value.severity() == StagingFindingSeverity.ERROR)
                .forEach(value -> blockers.add("Topology: " + value.code() + " - " + value.message()));

        return new StagingPromotionPreparation(
                staging.pullRequest().number(),
                staging.pullRequest().headSha(),
                staging.metadata().state(),
                !blockers.isEmpty(),
                children,
                checks,
                graph.findings(),
                blockers
        );
    }

    private static StagingBaseResolution resolved(
            RepositoryPullRequest parent,
            StagingBaseReason reason,
            List<StagingTopologyFinding> findings
    ) {
        return new StagingBaseResolution(
                true,
                parent.headBranch(),
                parent.number(),
                reason,
                List.of(parent.number()),
                findings
        );
    }

    private static RepositoryPullRequest requirePull(
            Map<Integer, RepositoryPullRequest> pulls,
            int number
    ) {
        RepositoryPullRequest pull = pulls.get(number);
        if (pull == null) {
            throw new IllegalArgumentException("Open pull request not found: " + number);
        }
        return pull;
    }

    private static EnumSet<StagingState> allowedTransitions(StagingState state) {
        return switch (state) {
            case ACTIVE -> EnumSet.of(StagingState.FROZEN, StagingState.SUPERSEDED);
            case FROZEN -> EnumSet.of(
                    StagingState.ACTIVE,
                    StagingState.PROMOTING,
                    StagingState.SUPERSEDED
            );
            case PROMOTING -> EnumSet.of(StagingState.ACTIVE, StagingState.SUPERSEDED);
            case SUPERSEDED -> EnumSet.noneOf(StagingState.class);
        };
    }

    private static boolean successfulConclusion(String conclusion) {
        String normalized = conclusion == null ? "" : conclusion.trim().toUpperCase();
        return Set.of("SUCCESS", "NEUTRAL", "SKIPPED").contains(normalized);
    }

    private static StagingTopologyFinding finding(
            String code,
            StagingFindingSeverity severity,
            String message,
            Integer number
    ) {
        return new StagingTopologyFinding(code, severity, message, number);
    }

    private static void detectCycles(
            List<StagingRelationship> relationships,
            List<StagingTopologyFinding> findings
    ) {
        Map<Integer, List<Integer>> children = new HashMap<>();
        for (StagingRelationship relationship : relationships) {
            children.computeIfAbsent(
                    relationship.parentPullRequestNumber(),
                    ignored -> new ArrayList<>()
            ).add(relationship.childPullRequestNumber());
        }
        Set<Integer> visited = new HashSet<>();
        Set<Integer> active = new HashSet<>();
        for (Integer node : children.keySet()) {
            if (cycle(node, children, visited, active)) {
                findings.add(finding(
                        "PULL_REQUEST_ANCESTRY_CYCLE",
                        StagingFindingSeverity.ERROR,
                        "Open pull request base graph contains a cycle",
                        node
                ));
                return;
            }
        }
    }

    private static boolean cycle(
            Integer node,
            Map<Integer, List<Integer>> children,
            Set<Integer> visited,
            Set<Integer> active
    ) {
        if (active.contains(node)) {
            return true;
        }
        if (!visited.add(node)) {
            return false;
        }
        active.add(node);
        for (Integer child : children.getOrDefault(node, List.of())) {
            if (cycle(child, children, visited, active)) {
                return true;
            }
        }
        active.remove(node);
        return false;
    }
}

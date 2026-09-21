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
        return inspectGraph(pulls);
    }

    private StagingGraph inspectGraph(List<RepositoryPullRequest> pulls) {
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
                .collect(Collectors.groupingBy(RepositoryStagingService::activeRootIdentity));
        for (Map.Entry<String, List<StagingPullRequest>> entry : activeGroups.entrySet()) {
            if (entry.getValue().size() > 1) {
                for (StagingPullRequest duplicate : entry.getValue()) {
                    findings.add(finding(
                            "DUPLICATE_ACTIVE_STAGING_ROOT",
                            StagingFindingSeverity.ERROR,
                            "Multiple active staging roots share identity " + entry.getKey(),
                            duplicate.pullRequest().number()
                    ));
                }
            }
        }

        validateMandatoryAncestry(pulls, byHead, staging, findings);

        detectCycles(relationships, findings);
        return new StagingGraph(
                pulls,
                staging,
                relationships,
                findings,
                provider.executionEvidence().orElse(null)
        );
    }

    private static String activeRootIdentity(StagingPullRequest value) {
        StagingMetadata metadata = value.metadata();
        if (metadata.type() == StagingType.DOMAIN_INTEGRATION) {
            return metadata.type() + "|" + metadata.branch();
        }
        return metadata.type() + "|" + metadata.parent();
    }

    /**
     * Enforces the repository integration invariant for every open normal PR
     * and every active sub-staging PR. A path is valid only when it reaches a
     * live staging root; a feature stack cannot be treated as integrated merely
     * because one of its branches happens to exist locally.
     */
    private static void validateMandatoryAncestry(
            List<RepositoryPullRequest> pulls,
            Map<String, RepositoryPullRequest> byHead,
            List<StagingPullRequest> staging,
            List<StagingTopologyFinding> findings
    ) {
        Map<Integer, StagingPullRequest> stagingByNumber = staging.stream()
                .collect(Collectors.toMap(
                        value -> value.pullRequest().number(),
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
        Map<Integer, Boolean> memo = new HashMap<>();
        Set<Integer> visiting = new HashSet<>();
        boolean activeStagingPresent = staging.stream()
                .anyMatch(value -> value.metadata().state() != StagingState.SUPERSEDED);
        for (RepositoryPullRequest pull : pulls) {
            if (hasLiveStagingPath(
                    pull, byHead, stagingByNumber, memo, visiting, findings, activeStagingPresent
            )) {
                continue;
            }
            if (stagingByNumber.containsKey(pull.number())) {
                continue;
            }
            findings.add(finding(
                    "PR_WITHOUT_STAGING_ANCESTRY",
                    StagingFindingSeverity.ERROR,
                    "Open normal pull request has no valid transitive path into an active Sub-Staging or Staging pull request",
                    pull.number()
            ));
        }
    }

    private static boolean hasLiveStagingPath(
            RepositoryPullRequest pull,
            Map<String, RepositoryPullRequest> byHead,
            Map<Integer, StagingPullRequest> stagingByNumber,
            Map<Integer, Boolean> memo,
            Set<Integer> visiting,
            List<StagingTopologyFinding> findings,
            boolean activeStagingPresent
    ) {
        Boolean known = memo.get(pull.number());
        if (known != null) {
            return known;
        }
        if (!visiting.add(pull.number())) {
            memo.put(pull.number(), false);
            return false;
        }

        StagingPullRequest staging = stagingByNumber.get(pull.number());
        boolean valid;
        if (staging != null && staging.metadata().state() == StagingState.SUPERSEDED) {
            // Superseded integration records are retained for history, but do
            // not count as active ancestry and do not themselves need repair.
            valid = false;
        } else if (staging != null) {
            StagingType type = staging.metadata().type();
            if ("main".equals(pull.baseBranch())
                    && (type == StagingType.REPOSITORY_INTEGRATION || type == StagingType.RELEASE_INTEGRATION)) {
                valid = true;
            } else {
                RepositoryPullRequest parent = byHead.get(pull.baseBranch());
                if (parent == null) {
                    findings.add(finding(
                            type == StagingType.DOMAIN_INTEGRATION
                                    ? "SUB_STAGING_WITHOUT_ACTIVE_PARENT"
                                    : "STAGING_PARENT_NOT_OPEN",
                            StagingFindingSeverity.ERROR,
                            "Live staging pull request does not have an open parent staging path",
                            pull.number()
                    ));
                    valid = false;
                } else {
                    valid = hasLiveStagingPath(
                            parent, byHead, stagingByNumber, memo, visiting, findings, activeStagingPresent
                    );
                }
            }
        } else {
            RepositoryPullRequest parent = byHead.get(pull.baseBranch());
            valid = parent != null
                    && hasLiveStagingPath(
                    parent, byHead, stagingByNumber, memo, visiting, findings, activeStagingPresent
            );
            if (!valid && activeStagingPresent && "main".equals(pull.baseBranch())) {
                findings.add(finding(
                        "DIRECT_TO_MAIN_WITH_ACTIVE_STAGING",
                        StagingFindingSeverity.ERROR,
                        "Open normal pull request targets main without a staging ancestry path",
                        pull.number()
                ));
            }
        }

        visiting.remove(pull.number());
        memo.put(pull.number(), valid);
        return valid;
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

    public RepositoryPullRequest createPullRequest(CreateStagedPullRequestRequest request) {
        List<RepositoryPullRequest> pulls = provider.listOpenPullRequests(request.repository());
        if (pulls.stream().anyMatch(pull -> pull.headBranch().equals(request.headBranch()))) {
            throw new IllegalStateException("An open pull request already owns the requested head branch");
        }
        requireHead(request.expectedHeadSha(), provider.branchHead(request.repository(), request.headBranch()).orElse(""));
        int candidateNumber = Math.addExact(pulls.stream().mapToInt(RepositoryPullRequest::number).max().orElse(0), 1);
        var candidate = new RepositoryPullRequest(candidateNumber, request.title(), request.body(),
                request.headBranch(), request.expectedHeadSha(), request.baseBranch(),
                provider.branchHead(request.repository(), request.baseBranch()).orElseThrow(
                        () -> new IllegalStateException("Requested base branch does not exist")), request.draft());
        List<RepositoryPullRequest> proposed = new ArrayList<>(pulls);
        proposed.add(candidate);
        requireValidGraph(proposed);
        RepositoryPullRequest created = provider.createPullRequest(request.repository(), request.title(),
                request.headBranch(), request.baseBranch(), request.body(), request.draft());
        verifyPublishedMutation(request.repository(), created, candidate);
        return created;
    }

    public RepositoryPullRequest updatePullRequest(UpdateStagedPullRequestRequest request) {
        List<RepositoryPullRequest> pulls = provider.listOpenPullRequests(request.repository());
        RepositoryPullRequest current = pulls.stream().filter(pull -> pull.number() == request.pullRequestNumber())
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Pull request is not open"));
        requireHead(request.expectedHeadSha(), current.headSha());
        if (request.baseBranch().isPresent() && !request.baseBranch().get().equals(current.baseBranch())) {
            pulls.stream().filter(pull -> pull.headBranch().equals(current.baseBranch())).findFirst().ifPresent(parent -> {
                if (StagingMetadataDocument.parse(parent.body()).metadata().isEmpty()) {
                    throw new IllegalStateException("Preserve the open feature dependency; reparent its stack root instead");
                }
            });
        }
        var candidate = new RepositoryPullRequest(current.number(), current.title(), request.body().orElse(current.body()),
                current.headBranch(), current.headSha(), request.baseBranch().orElse(current.baseBranch()),
                current.baseSha(), current.draft());
        requireValidGraph(pulls.stream().map(pull -> pull.number() == current.number() ? candidate : pull).toList());
        RepositoryPullRequest updated = provider.updatePullRequest(request.repository(), current.number(),
                request.baseBranch(), request.body());
        verifyPublishedMutation(request.repository(), updated, candidate);
        return updated;
    }

    private void verifyPublishedMutation(RepositoryCoordinates repository, RepositoryPullRequest changed, RepositoryPullRequest expected) {
        try {
            List<RepositoryPullRequest> observed = provider.listOpenPullRequests(repository);
            RepositoryPullRequest stored = observed.stream().filter(pull -> pull.number() == changed.number())
                    .findFirst().orElseThrow(() -> new IllegalStateException("Published pull request is not open"));
            requireHead(expected.headSha(), stored.headSha());
            if (!expected.headBranch().equals(stored.headBranch()) || !expected.baseBranch().equals(stored.baseBranch())
                    || !expected.body().equals(stored.body())) {
                throw new IllegalStateException("Published pull request differs from the validated mutation");
            }
            requireValidGraph(observed);
        } catch (RuntimeException failure) {
            throw new IllegalStateException("RECOVERY_REQUIRED: published PR #" + changed.number()
                    + " requires topology reconciliation before the workflow can succeed", failure);
        }
    }

    private void requireValidGraph(List<RepositoryPullRequest> pulls) {
        List<StagingTopologyFinding> errors = inspectGraph(pulls).findings().stream()
                .filter(finding -> finding.severity() == StagingFindingSeverity.ERROR).toList();
        if (!errors.isEmpty()) throw new IllegalStateException("Invalid staging topology: " + errors);
    }

    private static void requireHead(String expected, String actual) {
        if (!expected.equals(actual)) throw new IllegalStateException("STALE_SOURCE: pull request head changed");
    }

    public StagingEnsureResult ensure(EnsureStagingPullRequestRequest request) {
        StagingGraph graph = inspectGraph(new RepositoryStagingRequest(request.repository()));
        Optional<StagingPullRequest> existing = graph.stagingPullRequests().stream()
                .filter(value -> value.metadata().state() != StagingState.SUPERSEDED)
                .filter(value -> value.metadata().type() == request.type())
                .filter(value -> value.metadata().branch().equals(request.branch()))
                .findFirst();
        if (existing.isPresent()) {
            if (!existing.get().metadata().parent().equals(request.parentBranch())) {
                throw new IllegalStateException(
                        "Staging branch already exists with a different parent: " + existing.get().metadata().parent()
                );
            }
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
        if (source.number() == target.pullRequest().number()) {
            throw new IllegalArgumentException("A staging pull request cannot attach to itself");
        }
        if (target.metadata().state() != StagingState.ACTIVE) {
            throw new IllegalStateException("Only an ACTIVE staging pull request accepts new attachments");
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

        if (isDescendant(target.pullRequest(), source, byHead)) {
            throw new IllegalStateException("Attaching would create a pull-request ancestry cycle");
        }

        Optional<String> updatedBody = Optional.empty();
        StagingMetadataDocument sourceDocument = StagingMetadataDocument.parse(source.body());
        if (sourceDocument.malformed()) {
            throw new IllegalStateException("Cannot attach a pull request with malformed staging metadata");
        }
        if (sourceDocument.metadata().isPresent()) {
            StagingMetadata metadata = sourceDocument.metadata().get();
            updatedBody = Optional.of(sourceDocument.replace(new StagingMetadata(
                    metadata.type(),
                    metadata.state(),
                    metadata.branch(),
                    target.metadata().branch(),
                    metadata.promotion(),
                    metadata.childMergeTarget()
            )));
        }

        provider.updatePullRequest(
                request.repository(),
                source.number(),
                Optional.of(target.metadata().branch()),
                updatedBody
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
        if (request.state() == StagingState.SUPERSEDED) {
            boolean hasChildren = graph.pullRequests().stream()
                    .anyMatch(value -> value.baseBranch().equals(staging.pullRequest().headBranch()));
            if (hasChildren) {
                throw new IllegalStateException(
                        "Cannot supersede a staging pull request while open descendants still target it"
                );
            }
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

    private static boolean isDescendant(
            RepositoryPullRequest candidate,
            RepositoryPullRequest possibleAncestor,
            Map<String, RepositoryPullRequest> byHead
    ) {
        Set<String> visitedBranches = new HashSet<>();
        String branch = candidate.baseBranch();
        while (branch != null && visitedBranches.add(branch)) {
            if (branch.equals(possibleAncestor.headBranch())) {
                return true;
            }
            RepositoryPullRequest parent = byHead.get(branch);
            if (parent == null) {
                return false;
            }
            branch = parent.baseBranch();
        }
        return false;
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

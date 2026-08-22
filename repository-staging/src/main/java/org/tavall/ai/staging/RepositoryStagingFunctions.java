package org.tavall.ai.staging;

import org.tavall.ai.core.annotation.AIFunction;
import org.tavall.ai.core.annotation.AIParam;

import java.util.Objects;

/** Canonical typed staging functions. MCP/provider adapters project these definitions automatically. */
public final class RepositoryStagingFunctions {
    private final RepositoryStagingService service;

    public RepositoryStagingFunctions(RepositoryStagingService service) {
        this.service = Objects.requireNonNull(service, "service");
    }

    @AIFunction(name = "repository_staging_discover", description = "Discover Tavall staging PRs and metadata findings for a repository.")
    public StagingDiscoveryResult discover(@AIParam(name = "request") RepositoryStagingRequest request) {
        return service.scoped(request.context()).discover(request);
    }

    @AIFunction(name = "repository_staging_inspect_graph", description = "Inspect the open PR/staging ancestry graph without mutating it.")
    public StagingGraph inspectGraph(@AIParam(name = "request") RepositoryStagingRequest request) {
        return service.scoped(request.context()).inspectGraph(request);
    }

    @AIFunction(name = "repository_staging_resolve_base", description = "Resolve the correct PR base while preserving existing feature-stack ancestry.")
    public StagingBaseResolution resolveBase(@AIParam(name = "request") ResolveStagingBaseRequest request) {
        return service.scoped(request.context()).resolveBase(request);
    }

    @AIFunction(name = "repository_staging_validate", description = "Validate Tavall staging metadata and PR topology for unsafe or malformed states.")
    public StagingValidationResult validate(@AIParam(name = "request") RepositoryStagingRequest request) {
        return service.scoped(request.context()).validate(request);
    }

    @AIFunction(name = "repository_staging_ensure", description = "Find or create the requested Draft Tavall staging root from an explicit parent branch.")
    public StagingEnsureResult ensure(@AIParam(name = "request") EnsureStagingPullRequestRequest request) {
        return service.scoped(request.context()).ensure(request);
    }

    @AIFunction(name = "repository_staging_attach", description = "Attach an independent PR or stack root to a staging PR without flattening its descendants.")
    public StagingAttachResult attach(@AIParam(name = "request") AttachStagingPullRequestRequest request) {
        return service.scoped(request.context()).attach(request);
    }

    @AIFunction(name = "repository_staging_set_state", description = "Apply a validated Tavall staging state transition in tavall-staging:v1 metadata.")
    public StagingStateResult setState(@AIParam(name = "request") SetStagingStateRequest request) {
        return service.scoped(request.context()).setState(request);
    }

    @AIFunction(name = "repository_staging_prepare_promotion", description = "Collect exact-head staging promotion blockers/evidence without merging to main.")
    public StagingPromotionPreparation preparePromotion(@AIParam(name = "request") PrepareStagingPromotionRequest request) {
        return service.scoped(request.context()).preparePromotion(request);
    }
}

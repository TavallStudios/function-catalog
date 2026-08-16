# Bounded AI-Authored Function Catalog Tool Lifecycle

> **Document Type:** Working architecture
> **Engineering State:** `PLANNED_IMPLEMENTATION_NEEDS_CURRENT_REGISTRATION_AND_RUNTIME_CHARACTERIZATION`
> **Engineering Lane:** Linear TJ-215
> **Staging Parent:** `staging/platform`

## About

Tavall AI may identify a missing reusable capability and propose a typed Function Catalog/provider package, but generated code never grants itself execution authority. Function Catalog remains the machine-operable schema, policy, audit, invocation, version, and registration boundary; Tavall Cloud/CONTROL and existing policy layers remain authoritative for where and whether an operation may execute.

## Lifecycle

A generated tool moves through explicit product/runtime lifecycle states rather than appearing in the active catalog because a model emitted code:

1. proposal;
2. generated candidate package;
3. isolated build/test;
4. independent contract/security review;
5. versioned registration or quarantine;
6. bounded publication;
7. regression surveillance;
8. repair, rollback, supersession, or retirement.

Each transition produces reproducible evidence tied to the candidate version and implementation digest. These lifecycle states describe the generated capability itself; they are not idea traffic lights or PR readiness labels.

## Authority Rules

- A generator can propose implementation and metadata, never broaden its own Function Catalog view or invocation authority.
- A separate test/review role verifies inputs, outputs, side effects, failure behavior, blast radius, and authority boundaries.
- Generated candidates execute only in an explicitly authorized disposable development workspace/sandbox before registration.
- Registration does not imply Cloud/node/production mutation permission.
- MCP publication reuses the authoritative `AIFunctionCatalogView`; it must not create a second publication-only authority plane.
- Existing registered tools become regression fixtures so provider/runtime/dependency changes can trigger repeat validation.
- Failed or suspect versions can be quarantined or rolled back without deleting their historical evidence.

## Next Coherent Engineering Slice

After characterizing current registration, catalog-view, provider/runtime, versioning, and sandbox-placement seams:

- add typed generated-tool proposal/package metadata and lifecycle state;
- add reproducible build/test/review evidence contracts;
- add registration/version/quarantine/rollback semantics;
- add regression triggers for already-registered/generated tools;
- add a sandbox/publish boundary that cannot silently become arbitrary execution authority;
- add operator/API projection of generation, testing, review, registration, quarantine, and rollback state;
- add matching behavior/integration coverage for the production lifecycle boundary as it is implemented.

Do not create a separate ceremonial failure-only milestone for planned behavior. A naturally failing regression is appropriate when reproducing an observed defect in an existing capability; planned lifecycle behavior and its matching tests move together.

## Composition

This extends the current Function Catalog and agent runtime architecture. It must reuse `AIFunctionCatalogView`, provider-neutral `AIAgentRuntime`, runtime budgets/revocation, and development-only Tavall Cloud execution placement rather than introducing a self-authorizing agent subsystem.

## Explicit Unresolved Decisions

Supported implementation templates/languages, sandbox build images, signing/versioning details, promotion thresholds, protected-data scopes, deterministic integration environments, first-party graduation policy, and operator UX remain named architecture/product decisions. They do not define PR lifecycle, engineering authorization, or acceptance state.

## Validation State

Repository workflow plus current Function Catalog/agent-runtime architecture were inspected before this seed. No generated code execution, sandbox run, Java 25 validation, catalog registration, staging fan-in, `main` promotion, publication, release, or deployment is claimed.

## Promotion Boundary

This document records architecture and an engineering objective. It does not by itself authorize implementation, candidate execution, registration, publication, staging fan-in, `main` promotion, release, or deployment.

## Provenance

Canonical Notion fingerprint `tavall-ai:function-catalog:self-authoring-tool-lifecycle`; Linear TJ-215.

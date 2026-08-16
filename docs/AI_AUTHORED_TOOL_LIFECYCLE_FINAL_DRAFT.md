# Bounded AI-Authored Function Catalog Tool Lifecycle

> **Document Type:** Working design
> **Status:** Designed
> **Engineering Lane:** Linear TJ-215
> **Staging Parent:** `staging/platform`

## About

Tavall AI may identify a missing reusable capability and propose a typed Function Catalog/provider package, but generated code never grants itself execution authority. The Function Catalog remains the machine-operable schema, policy, audit, invocation, version, and registration boundary; Tavall Cloud/CONTROL and existing policy layers remain authoritative for where and whether an operation may execute.

## Lifecycle

A generated tool moves through explicit states rather than appearing in the active catalog because a model emitted code:

1. proposal;
2. generated candidate package;
3. isolated build/test;
4. independent contract/security review;
5. versioned registration or quarantine;
6. bounded publication;
7. regression surveillance;
8. repair, rollback, supersession, or retirement.

Each transition produces reproducible evidence tied to the candidate version and implementation digest.

## Authority Rules

- A generator can propose implementation and metadata, never broaden its own Function Catalog view or invocation authority.
- A separate test/review role verifies inputs, outputs, side effects, failure behavior, blast radius, and authority boundaries.
- Generated candidates execute only in an explicitly authorized disposable development workspace/sandbox before registration.
- Registration does not imply Cloud/node/production mutation permission.
- MCP publication reuses the authoritative `AIFunctionCatalogView`; it must not create a second publication-only authority plane.
- Existing registered tools become regression fixtures so provider/runtime/dependency changes can trigger repeat validation.
- Failed or suspect versions can be quarantined or rolled back without deleting their historical evidence.

## First Engineering Slice

- typed generated-tool proposal/package metadata and lifecycle states;
- reproducible build/test/review evidence contracts;
- registration/version/quarantine/rollback semantics;
- regression triggers for already-generated tools;
- sandbox/publish boundary that cannot silently become arbitrary host execution;
- operator/API projection of generation, testing, review, registration, quarantine, and rollback state.

## Composition

This extends the current Function Catalog and agent runtime architecture. It must reuse `AIFunctionCatalogView`, provider-neutral `AIAgentRuntime`, runtime budgets/revocation, and development-only Tavall Cloud execution placement rather than introducing a self-authorizing agent subsystem.

## Open Edges

Supported implementation templates/languages, sandbox build images, signing/versioning, promotion thresholds, secret scopes, deterministic integration environments, first-party graduation policy, and operator UX remain YELLOW and do not block typed lifecycle/evidence/quarantine contracts.

## Validation State

Repository workflow plus current Function Catalog/agent-runtime architecture were inspected before this seed. No generated code execution, sandbox run, Java 25 validation, catalog registration, staging fan-in, `main` promotion, publication, release, or deployment is claimed.

## Provenance

Canonical Notion fingerprint `tavall-ai:function-catalog:self-authoring-tool-lifecycle`; Linear TJ-215.

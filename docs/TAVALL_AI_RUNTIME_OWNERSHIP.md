# Tavall AI runtime ownership

> **Status:** Function Catalog owns callable-function infrastructure. Actual AI/model runtime and provider execution live in Tavall AI.

## Function Catalog owns

Function Catalog remains authoritative for:

- typed Java function definitions and registration;
- canonical parameter/result schema generation;
- `AIFunctionCatalog` and scoped `AIFunctionCatalogView` behavior;
- invocation routing and observed invocation counts;
- function policy/audit hooks;
- provider-neutral callable views;
- automatic MCP tool projection;
- model SDK helpers that are genuinely catalog/client integrations rather than execution runtimes.

The core dependency consumed by Tavall AI is `org.tavall:ai-core`.

## Tavall AI owns

Actual model execution belongs in `TavallStudios/tavall-ai-agent-task-manager`:

- `tavall-ai-runtime-model-execution`
  - provider-neutral model execution engine;
  - model budgets/timeouts;
  - authoritative Function Catalog view intersection;
  - actual observed tool-call accounting;
  - provider invocation/result semantics.

- `tavall-ai-runtime-codex`
  - Codex model/process adapter;
  - fixed Codex CLI construction;
  - environment filtering;
  - bounded capture/result mapping;
  - workspace and process-supervisor integration supplied by an authorized runtime host.

Reusable `tavall-agent-*` packages remain non-AI behavior/instruction/function-requirement packages loaded by the parent Tavall AI runtime.

## Removed Function Catalog modules

The former Function Catalog modules:

- `agent-runtime`
- `codex-agent-provider`

are removed from the active Gradle build and source tree. They were execution-runtime/provider implementations rather than function-catalog infrastructure.

Function Catalog does not keep compatibility copies of those modules. Carrying two live implementations would create ambiguous runtime ownership and inevitable drift.

## Cross-repository migration dependency

This removal is paired with Tavall AI PR #10, **Runtime: move model execution and Codex provider into Tavall AI**.

The Function Catalog removal PR must remain Draft / blocked from staging promotion until the Tavall AI replacement passes its owning Java 25 local verification and DEVELOPMENT Codex runtime acceptance. Source removal in this repository is not evidence that the replacement runtime is operational.

Function Catalog PR #11 independently adds repository-staging Java functions and MCP projection. Function Catalog PR #12 independently moves Function Catalog verification/release workflows to repository-local scripts. Reconciliation into `staging/platform` must combine those sibling build/settings changes rather than dropping any one sibling's intended module set.

## Authority boundary

Moving the Codex adapter to Tavall AI does not move ambient process authority into the adapter. Tavall Cloud or another explicitly authorized runtime host remains responsible for workspace leases, process isolation/supervision, executable/credential grants, cancellation, resource authority, and DEVELOPMENT eligibility.

Likewise, Function Catalog function names or agent metadata never grant repository or infrastructure authority by themselves. Execution-specific catalog views and provider/host policy remain authoritative.

## Verification

After the local-workflow consolidation in Function Catalog PR #12 is integrated, the canonical Function Catalog exact-head gate is:

```text
scripts/ci/verify
```

Until then, the equivalent Gradle acceptance remains:

```text
./gradlew --no-daemon clean check publishToMavenLocal stageRuntime
```

The cross-repository migration is complete only when both Function Catalog removal and Tavall AI replacement have been validated at their exact heads and reconciled into their staging trees.

# mcp-server Progression

> **Status:** Active progression record  
> **Document Type:** `PROGRESSION`  
> **Progression Scope:** `MODULE`  
> **Module Type:** `RUNTIME`  
> **Owning System:** `Function Catalog`  
> **Owns:** Audited implementation, integration, validation, and historical progression for `mcp-server`  
> **Does Not Own:** Aggregate system progression, deployment history, product/design rules, or Git workflow policy  
> **Audited Against:** `TavallStudios/function-catalog@6e27cbef4b6538896a81f4dd28eb3f0c3ea70530`  
> **Last Reconciled:** `2026-09-27 5:59 PM PDT`

## About

Owns the executable MCP application that publishes the Tavall function catalog through standalone HTTP and stdio server modes.

This record measures the runtime’s executable behavior, lifecycle, integration, and deployment acceptance.

## Module Context

| Field | Value |
| --- | --- |
| Repository | [TavallStudios/function-catalog](https://github.com/TavallStudios/function-catalog) |
| Module | `mcp-server` |
| Module Type | `RUNTIME` |
| Owning System | `Function Catalog` |
| System Progression | [Function Catalog System Progression](FUNCTION_CATALOG_SYSTEM_PROGRESSION.md) |
| Runtime Owner | `Self` |
| Primary Consumers | This module owns its executable process; no deployment acceptance was retrieved. |
| Current Branch / PR Stack | [mainline integration #10](https://github.com/TavallStudios/function-catalog/pull/10), [skill resource endpoint #29](https://github.com/TavallStudios/function-catalog/pull/29); documentation update: [PR #37](https://github.com/TavallStudios/function-catalog/pull/37). |
| Audited Revision | [`6e27cbef4b6538896a81f4dd28eb3f0c3ea70530`](https://github.com/TavallStudios/function-catalog/commit/6e27cbef4b6538896a81f4dd28eb3f0c3ea70530) on `main` |

## Current Status

| Field | State |
| --- | --- |
| Overall State | `PARTIAL` |
| Current Phase | Runnable application source is present; no execution or deployment acceptance is recorded. |
| Implementation | 6 tracked production Java source files; responsibility boundary is present in Gradle settings/root build configuration |
| Integration | Declared dependency graph and README relationships reviewed; consumer acceptance not verified |
| Validation | Source/build/test tree audited through GitHub; Gradle commands were not run in this docs-only pass |
| Runtime / Consumer Acceptance | Runtime exists in source; live/runtime acceptance not verified |
| Deployment Verification | Not verified; deployment status is owned by the system Deployment document |
| Primary Blocker | Module-local `.tavallci/ci.yaml` is absent in the audited `main` tree; 7 test source files are tracked but no execution result was retrieved |
| Next Slice | Add the required module CI definition and obtain test/consumer evidence appropriate to this module type |

## Progression Timeline

| Date / Time | State | Progression | Evidence | Result / Remaining Work |
| --- | --- | --- | --- | --- |
| 2026-04-06 2:23 AM PDT | `HISTORICAL_EVIDENCE` | Added: advance function catalog mcp server for initial commit | [5e223017e039](https://github.com/TavallStudios/function-catalog/commit/5e223017e039902a5e92537fe02110d8b1b1246a) | Current main has 6 production source source files, 7 `src/test` files, and 0 `src/integrationTest` files; no execution result is implied. |
| 2026-09-10 7:06 PM PDT | `IN_PROGRESS` | Extract reusable Function Catalog stdio MCP host | [1a622ca77c36](https://github.com/TavallStudios/function-catalog/commit/1a622ca77c367261a6638f1c9f9f9efa751d2058) | Current main has 6 production source source files, 7 `src/test` files, and 0 `src/integrationTest` files; no execution result is implied. |
| 2026-09-13 2:10 PM PDT | `IN_PROGRESS` | fix: preserve MCP invocation errors with projections (#32) | [6567a1c69847](https://github.com/TavallStudios/function-catalog/commit/6567a1c698478d90a1d17a9b510dc79ba7aafa6f) | Current main has 6 production source source files, 7 `src/test` files, and 0 `src/integrationTest` files; no execution result is implied. |

## Validation State

| Validation | State | Evidence | Remaining Work |
| --- | --- | --- | --- |
| Architecture / module boundary | Audited | `settings.gradle.kts`, root `build.gradle.kts`, source tree, module README at `6e27cbef4b65` | Reconcile future changes against module ownership |
| Unit | 7 tracked test source files; no test run result was retrieved. | Current source tree at [`6e27cbef4b65`](https://github.com/TavallStudios/function-catalog/tree/6e27cbef4b6538896a81f4dd28eb3f0c3ea70530/mcp-server) | Run the applicable Gradle test task |
| Integration | No module-local `src/integrationTest` sources were found; provider/runtime integration acceptance was not tested. | [Module tree](https://github.com/TavallStudios/function-catalog/tree/6e27cbef4b6538896a81f4dd28eb3f0c3ea70530/mcp-server) and build configuration | Run the declared integration/provider test boundary where applicable; record prerequisites |
| Consumer / Runtime | Not verified | Runtime source and existing README | Verify through the named runtime/consumer where applicable |
| End-to-End | Not verified | Current module/runtime documentation; no execution evidence | Record acceptance in the owning system Progression |

## Dependencies and Integration

| Dependency / Consumer | Relationship | State | Evidence |
| --- | --- | --- | --- |
| Root build and module source | Independent Gradle subproject | 6 tracked production Java source files; 7 files under `src/test`; no `src/integrationTest` files | [`settings.gradle.kts`](https://github.com/TavallStudios/function-catalog/blob/6e27cbef4b6538896a81f4dd28eb3f0c3ea70530/settings.gradle.kts), [module tree](https://github.com/TavallStudios/function-catalog/tree/6e27cbef4b6538896a81f4dd28eb3f0c3ea70530/mcp-server) |
| Module dependencies | Application module; API dependencies on `ai-core`, Jackson, SLF4J, and MCP; embedded Tomcat implementation dependency. | Declared in the root Gradle build; dependency resolution was not run | [`build.gradle.kts`](https://github.com/TavallStudios/function-catalog/blob/6e27cbef4b6538896a81f4dd28eb3f0c3ea70530/build.gradle.kts) |
| Runtime / primary consumer | Self | Executable boundary in source; no operational evidence | [Module README](../../mcp-server/README.md) |

## Blockers

| Blocker | Impact | Resolution |
| --- | --- | --- |
| Module-local `.tavallci/ci.yaml` is absent from audited main | Required per-module CI ownership is not represented on main | Add the module definition through a separate CI-scoped PR |
| Test sources are tracked but unexecuted | Passing behavior, provider compatibility, and operational acceptance cannot be claimed from file presence | Run configured Gradle checks and record their result; add missing scenarios if required |

## Next Slice

Add `.tavallci/ci.yaml` for `mcp-server` in a separate CI-scoped change, run the applicable build/test tasks, and verify the declared dependency/consumer edge. Record runtime/deployment acceptance in the owning system Deployment and Progression documents.

## Related Documentation

| Type | Document |
| --- | --- |
| Module README | [`README.md`](../../mcp-server/README.md) |
| Owning system Progression | [`FUNCTION_CATALOG_SYSTEM_PROGRESSION.md`](./FUNCTION_CATALOG_SYSTEM_PROGRESSION.md) |
| Build / source | [Root build](../../build.gradle.kts), [module source](../../mcp-server/src) |
| Deployment | See the owning system Deployment document. |

## Documentation Update State

<details>
<summary>Documentation Update State</summary>

### Current Locations

| Surface | Sync State | Location | Last Updated | Evidence |
| --- | --- | --- | --- | --- |
| GitHub | `PRIMARY` | `TavallStudios/function-catalog/docs/progression/MCP_SERVER_PROGRESSION.md` | 2026-09-27 5:59 PM PDT | Documentation branch `working/canonical-readme-module-docs-2026-09-27`, PR [#37](https://github.com/TavallStudios/function-catalog/pull/37); audited main baseline `6e27cbef4b6538896a81f4dd28eb3f0c3ea70530`. |
| Notion | `TEMPORARY_DRIFT` | Required twin not inspected | 2026-09-27 5:59 PM PDT | User-directed GitHub-only scope; synchronization remains pending. |

### Update History

| Timestamp | Surface | Event | Location | Previous Location | Evidence | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| 2026-09-27 5:59 PM PDT | GitHub | `CREATED` | `docs/progression/MCP_SERVER_PROGRESSION.md` | — | PR [#37](https://github.com/TavallStudios/function-catalog/pull/37); audited main `6e27cbef4b6538896a81f4dd28eb3f0c3ea70530` | Created module Progression from the current main source/build/history and module README. |

</details>

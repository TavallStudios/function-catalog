# tavall-function-catalog-mcp-runtime Progression

> **Status:** Active progression record  
> **Document Type:** `PROGRESSION`  
> **Progression Scope:** `MODULE`  
> **Module Type:** `RUNTIME`  
> **Owning System:** `Function Catalog`  
> **Owns:** Audited implementation, integration, validation, and historical progression for `tavall-function-catalog-mcp-runtime`  
> **Does Not Own:** Aggregate system progression, deployment history, product/design rules, or Git workflow policy  
> **Audited Against:** `TavallStudios/function-catalog`  
> **Last Reconciled:** `2026-11-18 2:30 AM UTC`

## About

Owns the executable MCP application that publishes the Tavall function catalog through standalone HTTP and stdio server modes. Conforms to `MODULE_TYPES.md` `-runtime` suffix rule for RUNTIME primary module types.

This record measures the runtime’s executable behavior, lifecycle, integration, and deployment acceptance.

## Module Context

| Field | Value |
| --- | --- |
| Repository | [TavallStudios/function-catalog](https://github.com/TavallStudios/function-catalog) |
| Module | `tavall-function-catalog-mcp-runtime` |
| Module Type | `RUNTIME` |
| Owning System | `Function Catalog` |
| System Progression | [Function Catalog System Progression](FUNCTION_CATALOG_SYSTEM_PROGRESSION.md) |
| Runtime Owner | `Self` |
| Primary Consumers | This module owns its executable process. |
| Current Branch / PR Stack | [mainline integration #10](https://github.com/TavallStudios/function-catalog/pull/10), [skill resource endpoint #29](https://github.com/TavallStudios/function-catalog/pull/29), [issue #39](https://github.com/TavallStudios/function-catalog/issues/39). |

## Current Status

| Field | State |
| --- | --- |
| Overall State | `IN_PROGRESS` |
| Current Phase | Executable module renamed to `tavall-function-catalog-mcp-runtime` adhering to MODULE_TYPES.md naming contract. |
| Implementation | 6 tracked production Java source files; responsibility boundary in Gradle settings and root build. |
| Integration | Declared dependency graph and README relationships reviewed. |
| Validation | `:tavall-function-catalog-mcp-runtime:test` passing on Java 25. |
| Runtime / Consumer Acceptance | Executable HTTP and stdio launchers verified. |
| Deployment Verification | Deployment record tracked in [TAVALL_FUNCTION_CATALOG_DEPLOYMENT.md](../../TAVALL_FUNCTION_CATALOG_DEPLOYMENT.md). |
| Primary Blocker | None. |
| Next Slice | CI gate validation and staging merge. |

## Progression Timeline

| Date / Time | State | Progression | Evidence | Result / Remaining Work |
| --- | --- | --- | --- | --- |
| 2026-04-06 2:23 AM PDT | `HISTORICAL_EVIDENCE` | Added: advance function catalog mcp server for initial commit | [5e223017e039](https://github.com/TavallStudios/function-catalog/commit/5e223017e039902a5e92537fe02110d8b1b1246a) | Historical initial commit |
| 2026-09-10 7:06 PM PDT | `HISTORICAL_EVIDENCE` | Extract reusable Function Catalog stdio MCP host | [1a622ca77c36](https://github.com/TavallStudios/function-catalog/commit/1a622ca77c367261a6638f1c9f9f9efa751d2058) | Reusable host |
| 2026-09-13 2:10 PM PDT | `HISTORICAL_EVIDENCE` | fix: preserve MCP invocation errors with projections (#32) | [6567a1c69847](https://github.com/TavallStudios/function-catalog/commit/6567a1c698478d90a1d17a9b510dc79ba7aafa6f) | MCP error projection |
| 2026-11-18 2:30 AM UTC | `IN_PROGRESS` | Rename mcp-server to tavall-function-catalog-mcp-runtime per MODULE_TYPES.md | [Issue #39](https://github.com/TavallStudios/function-catalog/issues/39) | Suffix contract and packaging updated |

## Validation State

| Validation | State | Evidence | Remaining Work |
| --- | --- | --- | --- |
| Architecture / module boundary | Audited | `settings.gradle.kts`, root `build.gradle.kts`, source tree, module README | Reconcile future changes against module ownership |
| Unit | PASS | 4 test suites passed (AIFunctionMcpServerTest, AIFunctionMcpStandaloneHttpServerTest, AIFunctionMcpToolPublisherTest, TavallSkillResourcePublisherTest) | Integration tests |
| Integration | Pending | Codex CLI integration test | Optional profile |
| Consumer / Runtime | Verified | Real launchers exist and run | Live staging deployment |
| End-to-End | Pending | Cross-repo validation | Staging integration |

## Dependencies and Integration

| Dependency / Consumer | Relationship | State | Evidence |
| --- | --- | --- | --- |
| Root build and module source | Independent Gradle subproject | 6 tracked production Java source files; 7 files under `src/test` | [`settings.gradle.kts`](https://github.com/TavallStudios/function-catalog/blob/main/settings.gradle.kts), [`tavall-function-catalog-mcp-runtime/`](../../tavall-function-catalog-mcp-runtime) |
| Module dependencies | Application module; API dependencies on `ai-core`, Jackson, SLF4J, and MCP; embedded Tomcat implementation dependency. | Declared in the root Gradle build | [`build.gradle.kts`](../../build.gradle.kts) |
| Runtime / primary consumer | Self | Executable boundary in source | [Module README](../../tavall-function-catalog-mcp-runtime/README.md) |

## Related Documentation

| Type | Document |
| --- | --- |
| Module README | [`README.md`](../../tavall-function-catalog-mcp-runtime/README.md) |
| Owning system Progression | [`FUNCTION_CATALOG_SYSTEM_PROGRESSION.md`](./FUNCTION_CATALOG_SYSTEM_PROGRESSION.md) |
| Build / source | [Root build](../../build.gradle.kts), [module source](../../tavall-function-catalog-mcp-runtime/src) |
| Deployment | See the owning system Deployment document. |

# Function Catalog System Progression

> **Status:** Active system progression record  
> **Document Type:** `PROGRESSION`  
> **Progression Scope:** `SYSTEM`  
> **System:** `Function Catalog`  
> **Owns:** Cross-module architecture, dependency boundaries, runtime integration, verification state, and system history  
> **Does Not Own:** Individual module implementation detail, product policy, or deployment facts that are not evidenced in GitHub  
> **Audited Against:** `TavallStudios/function-catalog@6e27cbef4b6538896a81f4dd28eb3f0c3ea70530`  
> **Last Reconciled:** `2026-09-27 5:59 PM PDT`

## About

Function Catalog is an eight-project Gradle system for typed function definitions and discovery, provider integrations, provider-neutral agent execution, and an executable MCP server. `ai-core` owns catalog contracts; `agent-runtime` and its providers own agent execution; `mcp-server` is the executable runtime boundary.

This record tracks cross-module progress and acceptance. Use each module Progression for its own source history and test evidence.

## System Context

| Field | Value |
| --- | --- |
| Repository | [TavallStudios/function-catalog](https://github.com/TavallStudios/function-catalog) |
| Audited main revision | [`6e27cbef4b6538896a81f4dd28eb3f0c3ea70530`](https://github.com/TavallStudios/function-catalog/commit/6e27cbef4b6538896a81f4dd28eb3f0c3ea70530) |
| Build | Gradle multi-project; JDK 25 |
| Runtime boundary | [`mcp-server`](../../mcp-server/README.md), executable over HTTP or stdio |
| Current PR stack | Mainline integration [#10](https://github.com/TavallStudios/function-catalog/pull/10), MCP skill resources [#29](https://github.com/TavallStudios/function-catalog/pull/29), runtime/provider proposal [#13](https://github.com/TavallStudios/function-catalog/pull/13), and documentation [#37](https://github.com/TavallStudios/function-catalog/pull/37); separate product PRs [#19](https://github.com/TavallStudios/function-catalog/pull/19) and [#21](https://github.com/TavallStudios/function-catalog/pull/21) also appear in module README routing. |
| Overall state | `PARTIAL` |

## Current Status

| Area | State |
| --- | --- |
| Module boundaries | Eight independently included Gradle subprojects are documented below. The root build is an aggregator. |
| Implementation | 73 production Java source files are tracked across the eight modules. |
| Verification | 46 source files are tracked under `src/test`; no Gradle task or test suite was run in this documentation pass. Source presence is not a passing result. |
| CI ownership | No module-local `.tavallci/ci.yaml` was found in the audited main tree. |
| Runtime integration | `mcp-server` is configured as an application with HTTP and stdio launch paths. No runtime or consumer acceptance was verified. |
| Deployment | GitHub deployment target and deployed source are not established by the evidence audited for this rollout; see the [system Deployment record](../TAVALL_FUNCTION_CATALOG_DEPLOYMENT.md). |
| Current blocker | Module CI definitions and execution evidence are absent; provider/runtime and deployed-source acceptance remain unverified. |

## Module Map

| Module | Type | Runtime owner | Boundary | Module Progression |
| --- | --- | --- | --- | --- |
| [`gemini-sdk`](../../gemini-sdk/README.md) | `INTEGRATION` | None | Gemini 3 text/image client wrappers, response models, and parsing | [Progression](GEMINI_SDK_PROGRESSION.md) |
| [`ai-core`](../../ai-core/README.md) | `LIBRARY` | `mcp-server` | Typed function catalog, registration, scoped views, schemas, and audit | [Progression](AI_CORE_PROGRESSION.md) |
| [`agent-runtime`](../../agent-runtime/README.md) | `LIBRARY` | None | Provider-neutral job execution, budgets, timeouts, and cancellation | [Progression](AGENT_RUNTIME_PROGRESSION.md) |
| [`codex-agent-provider`](../../codex-agent-provider/README.md) | `PROVIDER` | `agent-runtime` | Codex agent provider | [Progression](CODEX_AGENT_PROVIDER_PROGRESSION.md) |
| [`strands-agent-provider`](../../strands-agent-provider/README.md) | `PROVIDER` | `agent-runtime` | Strands agent provider through the MCP client boundary | [Progression](STRANDS_AGENT_PROVIDER_PROGRESSION.md) |
| [`openai-sdk`](../../openai-sdk/README.md) | `INTEGRATION` | None | OpenAI client integration | [Progression](OPENAI_SDK_PROGRESSION.md) |
| [`claude-sdk`](../../claude-sdk/README.md) | `INTEGRATION` | None | Anthropic Claude client integration | [Progression](CLAUDE_SDK_PROGRESSION.md) |
| [`mcp-server`](../../mcp-server/README.md) | `RUNTIME` | Self | Executable HTTP and stdio MCP server | [Progression](MCP_SERVER_PROGRESSION.md) |

## Dependencies and Integration

| Boundary | Relationship | Evidence / state |
| --- | --- | --- |
| `ai-core` | Jackson, SLF4J, and ClassGraph dependencies support catalog contracts and discovery. | Declared in the root Gradle build; dependency resolution was not run. |
| `agent-runtime` | API dependency on `ai-core`; Jackson and SLF4J support provider-neutral jobs. | Declared in the root Gradle build; behavior was not executed. |
| Agent providers | `codex-agent-provider` depends on `agent-runtime`; `strands-agent-provider` also uses the MCP client boundary. | Provider acceptance remains unverified. |
| SDK integrations | Gemini uses Google GenAI; OpenAI and Claude depend on the shared catalog contracts and JSON/logging libraries. | External provider APIs and credentials were not exercised. |
| `mcp-server` | Application boundary over `ai-core`, MCP libraries, Jackson/SLF4J, and embedded Tomcat. | HTTP/stdio execution and consumer acceptance were not tested. |

## Progression Timeline

| Date / Time | State | Progression | Evidence | Result / Remaining Work |
| --- | --- | --- | --- | --- |
| 2026-04-06 2:11 AM PDT | `HISTORICAL_EVIDENCE` | Initial catalog core entered the repository history. | [`c5b31cfba614`](https://github.com/TavallStudios/function-catalog/commit/c5b31cfba614087a21c104cae60d780d8529be67) | Current main has eight modules; test and runtime acceptance were not established by this historical event. |
| 2026-08-08 6:55 PM PDT | `IN_PROGRESS` | Added the provider-neutral Tavall agent runtime. | [`76b1c3da7f6b`](https://github.com/TavallStudios/function-catalog/commit/76b1c3da7f6bd8e2f22fbe110c0f2d6996346e2b) | Agent execution gained a separate boundary; provider and consumer acceptance remained outstanding. |
| 2026-08-10 6:52 PM PDT | `IN_PROGRESS` | Added supervised Codex provider execution. | [`b95975ec6c99`](https://github.com/TavallStudios/function-catalog/commit/b95975ec6c99d82110b76d17e45c921fc46139d7) | Codex provider is separate from the provider-neutral runtime; tests were not run in this audit. |
| 2026-09-10 10:52 AM PDT | `IN_PROGRESS` | Configured a standalone Strands MCP process. | [`81fbced704f4`](https://github.com/TavallStudios/function-catalog/commit/81fbced704f449ddee3d2059ee2ea4898b898da7) | Added another provider integration boundary; its tracked source tree has no module-local tests. |
| 2026-09-10 7:06 PM PDT | `IN_PROGRESS` | Extracted a reusable MCP stdio host. | [`1a622ca77c36`](https://github.com/TavallStudios/function-catalog/commit/1a622ca77c367261a6638f1c9f9f9efa751d2058) | The runtime supports a reusable stdio launch boundary; no launch was performed here. |
| 2026-09-11 10:04 AM PDT | `IN_PROGRESS` | Released the catalog monitor during function invocation. | [`9ba07aaffecb`](https://github.com/TavallStudios/function-catalog/commit/9ba07aaffecb63234494dfa619a097e10ad8d2e6) | Source history records a monitor fix; regression tests were not executed in this pass. |
| 2026-09-13 8:25 AM PDT | `IN_PROGRESS` | Added a bounded timeout for Strands MCP requests. | [`3bbf667cef69`](https://github.com/TavallStudios/function-catalog/commit/3bbf667cef690e0970b410c90ebccdf3dac989fc) | Request timeout behavior is represented in source history; provider acceptance remains unverified. |
| 2026-09-13 2:10 PM PDT | `IN_PROGRESS` | Preserved MCP invocation errors through projections. | [`6567a1c69847`](https://github.com/TavallStudios/function-catalog/commit/6567a1c698478d90a1d17a9b510dc79ba7aafa6f) | Error projection changed in source history; no runtime or test result is claimed. |

## Validation State

| Validation | State | Evidence | Remaining Work |
| --- | --- | --- | --- |
| Module map and build boundaries | Audited | Main `settings.gradle.kts`, root build, source paths, and module READMEs | Reconcile future module/build changes in the system and module records |
| Unit and integration tests | Not run | 46 tracked `src/test` files; no test result was retrieved | Run the configured Gradle checks in an approved environment |
| Provider compatibility | Not verified | SDK/provider dependencies are declared; no external API calls were run | Verify each provider boundary with suitable test credentials |
| MCP runtime | Not run | Application configuration and HTTP/stdio source paths are present | Exercise the runtime and record consumer acceptance |
| Deployment | Not verified | Current GitHub deployment evidence did not establish target/source | Record an evidence-backed deployment state in the Deployment record |
| Module CI | Missing in audited main | No module `.tavallci/ci.yaml` definitions were present in the audited tree | Add CI definitions through a separate CI-scoped change |

## Blockers

| Blocker | Impact | Resolution |
| --- | --- | --- |
| Module-level CI and executed verification are absent from this audit. | Automated verification and passing behavior cannot be claimed from source/test files alone. | Add module CI definitions, run the configured checks, and record the results. |
| Provider and MCP runtime acceptance is unverified. | External provider behavior, process launch, and client compatibility remain unknown. | Exercise each boundary in an approved test environment and record evidence. |
| Deployment target and deployed source are not established in GitHub evidence reviewed. | The live system state cannot be reported from this record. | Reconcile the system Deployment document against GitHub deployment evidence. |

## Next Slice

Add the required module CI definitions through a separate CI-scoped change, execute the module checks and provider/runtime scenarios in approved environments, then update this system record with results. Record deployment only when GitHub evidence establishes the target and deployed source.

<details>
<summary>Documentation Update State</summary>

| Surface | Sync State | Location | Last Updated | Evidence |
| --- | --- | --- | --- | --- |
| GitHub | `PRIMARY` | `TavallStudios/function-catalog/docs/progression/FUNCTION_CATALOG_SYSTEM_PROGRESSION.md` | 2026-09-27 5:59 PM PDT | Documentation branch `working/canonical-readme-module-docs-2026-09-27`, PR [#37](https://github.com/TavallStudios/function-catalog/pull/37); audited main `6e27cbef4b6538896a81f4dd28eb3f0c3ea70530`. |
| Notion | `TEMPORARY_DRIFT` | Required twin not inspected | 2026-09-27 5:59 PM PDT | User-directed GitHub-only scope; synchronization remains pending. |

### Update History

| Timestamp | Surface | Event | Location | Previous Location | Evidence | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| 2026-09-27 5:59 PM PDT | GitHub | `CREATED` | `docs/progression/FUNCTION_CATALOG_SYSTEM_PROGRESSION.md` | — | PR [#37](https://github.com/TavallStudios/function-catalog/pull/37); audited main `6e27cbef4b6538896a81f4dd28eb3f0c3ea70530`. | Created system Progression from current GitHub module, build, and source-history evidence. |

</details>

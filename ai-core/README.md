# ai-core

Owns typed Tavall function definitions, parameter/publication metadata, registration, catalog views, and audit extension points.

## Responsibility

### Owns
- Function and parameter definitions plus registration/bootstrap APIs.
- Scoped catalog views used for discovery and invocation.
- Publication-schema and audit extension points.

### Does Not Own
- Model selection, planning, memory, or long-lived agent execution.
- Cloud routing, machine placement, or MCP server lifecycle.

## Repository Structure

function-catalog/
├── [`gemini-sdk`](../gemini-sdk/README.md)
├── **[`ai-core`](README.md) ← This Module**
├── [`agent-runtime`](../agent-runtime/README.md)
├── [`codex-agent-provider`](../codex-agent-provider/README.md)
├── [`strands-agent-provider`](../strands-agent-provider/README.md)
├── [`openai-sdk`](../openai-sdk/README.md)
├── [`claude-sdk`](../claude-sdk/README.md)
└── [`mcp-server`](../mcp-server/README.md)

## Relationships

| Module / System | Relationship |
| --- | --- |
| [`agent-runtime`](../agent-runtime/README.md) | Uses the catalog to resolve and narrow per-job function views. |
| [`mcp-server`](../mcp-server/README.md) | Publishes registered catalog functions through MCP. |
| [`gemini-sdk`](../gemini-sdk/README.md) | Separate provider client integration. |

## Documentation

| Type | Document | Purpose | Surface |
| --- | --- | --- | --- |
| Technical | [Function Catalog architecture](../docs/AGENT_FUNCTION_CATALOG_ARCHITECTURE.md) | Owns catalog functions, scoped views, and system boundaries. | GitHub |
| Progression | [Ai Core Progression](../docs/progression/AI_CORE_PROGRESSION.md) | Module implementation, validation, and history. | GitHub |
| Progression | [Function Catalog System Progression](../docs/progression/FUNCTION_CATALOG_SYSTEM_PROGRESSION.md) | Cross-module architecture and system acceptance. | GitHub |

## Deployment

> This module is not independently deployed.

Runtime owner: [`mcp-server`](../mcp-server/README.md). Deployment record: [TAVALL_FUNCTION_CATALOG_DEPLOYMENT.md](../TAVALL_FUNCTION_CATALOG_DEPLOYMENT.md).

## Development

- **Module Type:** `LIBRARY`
- **Runtime:** `mcp-server`
- **Current PR Stack:** [mainline integration #10](https://github.com/TavallStudios/function-catalog/pull/10), [WorldOps contract #19](https://github.com/TavallStudios/function-catalog/pull/19), [product intelligence #21](https://github.com/TavallStudios/function-catalog/pull/21); documentation update: [PR #37](https://github.com/TavallStudios/function-catalog/pull/37).
- Shared contribution policy: [Tavall Docs Git Workflow](https://github.com/TavallStudios/tavall-docs/blob/main/docs/quality/GIT_WORKFLOW.md).

- **Progression:** [Module Progression](../docs/progression/AI_CORE_PROGRESSION.md) · [System Progression](../docs/progression/FUNCTION_CATALOG_SYSTEM_PROGRESSION.md).
- **Module CI:** Missing in audited main: `.tavallci/ci.yaml`.

<details>
<summary>Documentation Update State</summary>

### Current Locations

| Surface | Sync State | Location | Last Updated | Evidence |
| --- | --- | --- | --- | --- |
| GitHub | `PRIMARY` | `TavallStudios/function-catalog/ai-core/README.md` | 2026-09-27 5:59 PM PDT | [PR #37](https://github.com/TavallStudios/function-catalog/pull/37) |
| Notion | `NOT_APPLICABLE` | — | 2026-09-27 12:59 PM PDT | README routing surface; no 1:1 twin is assigned. |

### Update History

| Timestamp | Surface | Event | Location | Previous Location | Evidence | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| 2026-09-27 12:59 PM PDT | GitHub | `UPDATED` | `TavallStudios/function-catalog/ai-core/README.md` | `TavallStudios/function-catalog/ai-core/README.md` | [PR #37](https://github.com/TavallStudios/function-catalog/pull/37) | Canonicalized module ownership, runtime, and current PR routing. |
| 2026-09-27 5:59 PM PDT | GitHub | `UPDATED` | `TavallStudios/function-catalog/ai-core/README.md` | `TavallStudios/function-catalog/ai-core/README.md` | [PR #37](https://github.com/TavallStudios/function-catalog/pull/37) | Added module and System Progression routes and recorded module CI state. |

</details>

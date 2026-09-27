# mcp-server

Owns the executable MCP application that publishes the Tavall function catalog through standalone HTTP and stdio server modes.

## Responsibility

### Owns
- HTTP/stdio server launchers and application lifecycle.
- Mapping published catalog functions to MCP tools and protocol messages.

### Does Not Own
- Catalog definitions, authorization policy, Cloud routing, or agent-provider selection.
- A hosted endpoint unless a deployment target is recorded separately.

## Repository Structure

function-catalog/
├── [`gemini-sdk`](../gemini-sdk/README.md)
├── [`ai-core`](../ai-core/README.md)
├── [`agent-runtime`](../agent-runtime/README.md)
├── [`codex-agent-provider`](../codex-agent-provider/README.md)
├── [`strands-agent-provider`](../strands-agent-provider/README.md)
├── [`openai-sdk`](../openai-sdk/README.md)
├── [`claude-sdk`](../claude-sdk/README.md)
└── **[`mcp-server`](README.md) ← This Module**

## Relationships

| Module / System | Relationship |
| --- | --- |
| [`ai-core`](../ai-core/README.md) | Loads and publishes registered catalog functions. |
| [`agent-runtime`](../agent-runtime/README.md) | Separate library; it does not own the MCP transport lifecycle. |

## Documentation

| Type | Document | Purpose | Surface |
| --- | --- | --- | --- |
| Technical | [Function Catalog architecture](../docs/AGENT_FUNCTION_CATALOG_ARCHITECTURE.md) | Owns catalog functions, scoped views, and system boundaries. | GitHub |

## Deployment

> This module is the independently executable runtime.

The module owns the independent MCP process. Its target and source are not established by current GitHub deployment records. See [TAVALL_FUNCTION_CATALOG_DEPLOYMENT.md](../TAVALL_FUNCTION_CATALOG_DEPLOYMENT.md).

## Development

- **Module Type:** `RUNTIME`
- **Runtime:** `Self`
- **Current PR Stack:** [mainline integration #10](https://github.com/TavallStudios/function-catalog/pull/10), [skill resource endpoint #29](https://github.com/TavallStudios/function-catalog/pull/29); documentation update: [PR #37](https://github.com/TavallStudios/function-catalog/pull/37).
- Shared contribution policy: [Tavall Docs Git Workflow](https://github.com/TavallStudios/tavall-docs/blob/main/docs/quality/GIT_WORKFLOW.md).


<details>
<summary>Documentation Update State</summary>

### Current Locations

| Surface | Sync State | Location | Last Updated | Evidence |
| --- | --- | --- | --- | --- |
| GitHub | `PRIMARY` | `TavallStudios/function-catalog/mcp-server/README.md` | 2026-09-27 12:59 PM PDT | [PR #37](https://github.com/TavallStudios/function-catalog/pull/37) |
| Notion | `NOT_APPLICABLE` | — | 2026-09-27 12:59 PM PDT | README routing surface; no 1:1 twin is assigned. |

### Update History

| Timestamp | Surface | Event | Location | Previous Location | Evidence | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| 2026-09-27 12:59 PM PDT | GitHub | `CREATED` | `TavallStudios/function-catalog/mcp-server/README.md` | — | [PR #37](https://github.com/TavallStudios/function-catalog/pull/37) | Canonicalized module ownership, runtime, and current PR routing. |

</details>

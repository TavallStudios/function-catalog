# strands-agent-provider

Implements the agent-runtime provider boundary for the Strands bridge, with MCP tool references, invocation limits, and observed events.

## Responsibility

### Owns
- Strands provider invocation and MCP bridge integration.
- Provider limits and normalized tool results/events.

### Does Not Own
- The shared runtime/catalog or Tavall Cloud deployment boundary.
- A standalone Strands server.

## Repository Structure

function-catalog/
├── [`gemini-sdk`](../gemini-sdk/README.md)
├── [`ai-core`](../ai-core/README.md)
├── [`agent-runtime`](../agent-runtime/README.md)
├── [`codex-agent-provider`](../codex-agent-provider/README.md)
├── **[`strands-agent-provider`](README.md) ← This Module**
├── [`openai-sdk`](../openai-sdk/README.md)
├── [`claude-sdk`](../claude-sdk/README.md)
└── [`mcp-server`](../mcp-server/README.md)

## Relationships

| Module / System | Relationship |
| --- | --- |
| [`agent-runtime`](../agent-runtime/README.md) | Implements the provider interface consumed by the runtime. |
| [`mcp-server`](../mcp-server/README.md) | Shares MCP protocol concerns without owning the catalog server. |

## Documentation

| Type | Document | Purpose | Surface |
| --- | --- | --- | --- |
| Technical | [Agent runtime architecture](../docs/TAVALL_AGENT_RUNTIME_ARCHITECTURE.md) | Owns provider-neutral job execution and runtime responsibilities. | GitHub |

## Deployment

> This module is not independently deployed.

Runtime owner: [`agent-runtime`](../agent-runtime/README.md). No Deployment record applies to this provider boundary.

## Development

- **Module Type:** `PROVIDER`
- **Runtime:** `agent-runtime`
- **Current PR Stack:** [mainline integration #10](https://github.com/TavallStudios/function-catalog/pull/10), [runtime/provider ownership proposal #13](https://github.com/TavallStudios/function-catalog/pull/13); documentation update: __PR_LINK__.
- Shared contribution policy: [Tavall Docs Git Workflow](https://github.com/TavallStudios/tavall-docs/blob/main/docs/quality/GIT_WORKFLOW.md).


<details>
<summary>Documentation Update State</summary>

### Current Locations

| Surface | Sync State | Location | Last Updated | Evidence |
| --- | --- | --- | --- | --- |
| GitHub | `PRIMARY` | `TavallStudios/function-catalog/strands-agent-provider/README.md` | 2026-09-27 12:51 PM PDT | __PR_URL__ |
| Notion | `NOT_APPLICABLE` | — | 2026-09-27 12:51 PM PDT | README routing surface; no 1:1 twin is assigned. |

### Update History

| Timestamp | Surface | Event | Location | Previous Location | Evidence | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| 2026-09-27 12:51 PM PDT | GitHub | `UPDATED` | `TavallStudios/function-catalog/strands-agent-provider/README.md` | `TavallStudios/function-catalog/strands-agent-provider/README.md` | __PR_URL__ | Canonicalized module ownership, runtime, and current PR routing. |

</details>

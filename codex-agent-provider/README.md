# codex-agent-provider

Implements the agent-runtime provider boundary with Codex configuration, command construction, workspace resolution, and process supervision.

## Responsibility

### Owns
- Codex-specific provider invocation and configuration.
- Command, workspace, sandbox-mode, and process-isolation behavior.

### Does Not Own
- The provider-neutral job contract or catalog policy.
- Machine placement or a deployed Codex worker.

## Repository Structure

function-catalog/
├── [`gemini-sdk`](../gemini-sdk/README.md)
├── [`ai-core`](../ai-core/README.md)
├── [`agent-runtime`](../agent-runtime/README.md)
├── **[`codex-agent-provider`](README.md) ← This Module**
├── [`strands-agent-provider`](../strands-agent-provider/README.md)
├── [`openai-sdk`](../openai-sdk/README.md)
├── [`claude-sdk`](../claude-sdk/README.md)
└── [`mcp-server`](../mcp-server/README.md)

## Relationships

| Module / System | Relationship |
| --- | --- |
| [`agent-runtime`](../agent-runtime/README.md) | Implements the provider interface consumed by the runtime. |
| [`ai-core`](../ai-core/README.md) | Consumes a scoped view passed by the runtime. |

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
- **Current PR Stack:** [mainline integration #10](https://github.com/TavallStudios/function-catalog/pull/10), [runtime/provider ownership proposal #13](https://github.com/TavallStudios/function-catalog/pull/13); documentation update: [PR #37](https://github.com/TavallStudios/function-catalog/pull/37).
- Shared contribution policy: [Tavall Docs Git Workflow](https://github.com/TavallStudios/tavall-docs/blob/main/docs/quality/GIT_WORKFLOW.md).


<details>
<summary>Documentation Update State</summary>

### Current Locations

| Surface | Sync State | Location | Last Updated | Evidence |
| --- | --- | --- | --- | --- |
| GitHub | `PRIMARY` | `TavallStudios/function-catalog/codex-agent-provider/README.md` | 2026-09-27 12:59 PM PDT | [PR #37](https://github.com/TavallStudios/function-catalog/pull/37) |
| Notion | `NOT_APPLICABLE` | — | 2026-09-27 12:59 PM PDT | README routing surface; no 1:1 twin is assigned. |

### Update History

| Timestamp | Surface | Event | Location | Previous Location | Evidence | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| 2026-09-27 12:59 PM PDT | GitHub | `UPDATED` | `TavallStudios/function-catalog/codex-agent-provider/README.md` | `TavallStudios/function-catalog/codex-agent-provider/README.md` | [PR #37](https://github.com/TavallStudios/function-catalog/pull/37) | Canonicalized module ownership, runtime, and current PR routing. |

</details>

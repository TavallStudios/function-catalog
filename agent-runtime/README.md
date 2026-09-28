# agent-runtime

Owns provider-neutral execution for an agent/job, including function-view resolution, execution budgets, timeouts, cancellation, and result reporting.

## Responsibility

### Owns
- Agent definitions, job requests, execution budgets/results, and provider-neutral lifecycle.
- Fail-closed function-view narrowing, invocation counting, timeout, and view revocation.

### Does Not Own
- A long-lived worker process or independent deployment.
- Provider-specific process/API behavior, machine placement, or Cloud authorization.

## Repository Structure

function-catalog/
├── [`gemini-sdk`](../gemini-sdk/README.md)
├── [`ai-core`](../ai-core/README.md)
├── **[`agent-runtime`](README.md) ← This Module**
├── [`codex-agent-provider`](../codex-agent-provider/README.md)
├── [`strands-agent-provider`](../strands-agent-provider/README.md)
├── [`openai-sdk`](../openai-sdk/README.md)
├── [`claude-sdk`](../claude-sdk/README.md)
└── [`mcp-server`](../mcp-server/README.md)

## Relationships

| Module / System | Relationship |
| --- | --- |
| [`ai-core`](../ai-core/README.md) | Resolves the exact catalog view supplied to a job. |
| [`codex-agent-provider`](../codex-agent-provider/README.md) | Implements the provider interface. |
| [`strands-agent-provider`](../strands-agent-provider/README.md) | Implements the provider interface. |

## Documentation

| Type | Document | Purpose | Surface |
| --- | --- | --- | --- |
| Technical | [Agent runtime architecture](../docs/TAVALL_AGENT_RUNTIME_ARCHITECTURE.md) | Owns provider-neutral job execution and runtime responsibilities. | GitHub |
| Progression | [Agent Runtime Progression](../docs/progression/AGENT_RUNTIME_PROGRESSION.md) | Module implementation, validation, and history. | GitHub |
| Progression | [Function Catalog System Progression](../docs/progression/FUNCTION_CATALOG_SYSTEM_PROGRESSION.md) | Cross-module architecture and system acceptance. | GitHub |

## Deployment

> This module is not independently deployed.

No independent runtime owner or Deployment record applies to this integration.

## Development

- **Module Type:** `LIBRARY`
- **Runtime:** `None`
- **Current PR Stack:** [mainline integration #10](https://github.com/TavallStudios/function-catalog/pull/10), [runtime/provider ownership proposal #13](https://github.com/TavallStudios/function-catalog/pull/13); documentation update: [PR #37](https://github.com/TavallStudios/function-catalog/pull/37).
- Shared contribution policy: [Tavall Docs Git Workflow](https://github.com/TavallStudios/tavall-docs/blob/main/docs/quality/GIT_WORKFLOW.md).

- **Progression:** [Module Progression](../docs/progression/AGENT_RUNTIME_PROGRESSION.md) · [System Progression](../docs/progression/FUNCTION_CATALOG_SYSTEM_PROGRESSION.md).
- **Module CI:** Missing in audited main: `.tavallci/ci.yaml`.

<details>
<summary>Documentation Update State</summary>

### Current Locations

| Surface | Sync State | Location | Last Updated | Evidence |
| --- | --- | --- | --- | --- |
| GitHub | `PRIMARY` | `TavallStudios/function-catalog/agent-runtime/README.md` | 2026-09-27 5:59 PM PDT | [PR #37](https://github.com/TavallStudios/function-catalog/pull/37) |
| Notion | `NOT_APPLICABLE` | — | 2026-09-27 12:59 PM PDT | README routing surface; no 1:1 twin is assigned. |

### Update History

| Timestamp | Surface | Event | Location | Previous Location | Evidence | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| 2026-09-27 12:59 PM PDT | GitHub | `UPDATED` | `TavallStudios/function-catalog/agent-runtime/README.md` | `TavallStudios/function-catalog/agent-runtime/README.md` | [PR #37](https://github.com/TavallStudios/function-catalog/pull/37) | Canonicalized module ownership, runtime, and current PR routing. |
| 2026-09-27 5:59 PM PDT | GitHub | `UPDATED` | `TavallStudios/function-catalog/agent-runtime/README.md` | `TavallStudios/function-catalog/agent-runtime/README.md` | [PR #37](https://github.com/TavallStudios/function-catalog/pull/37) | Added module and System Progression routes and recorded module CI state. |

</details>

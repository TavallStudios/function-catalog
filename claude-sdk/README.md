# claude-sdk

Owns the Anthropic Java client wrapper and tool-call/result adaptation used by callers.

## Responsibility

### Owns
- Anthropic client integration, tool adapter/parser, and result types.

### Does Not Own
- Provider-neutral agent execution or Tavall function definitions.
- Application-level provider selection or deployment.

## Repository Structure

function-catalog/
├── [`gemini-sdk`](../gemini-sdk/README.md)
├── [`ai-core`](../ai-core/README.md)
├── [`agent-runtime`](../agent-runtime/README.md)
├── [`codex-agent-provider`](../codex-agent-provider/README.md)
├── [`strands-agent-provider`](../strands-agent-provider/README.md)
├── [`openai-sdk`](../openai-sdk/README.md)
├── **[`claude-sdk`](README.md) ← This Module**
└── [`mcp-server`](../mcp-server/README.md)

## Relationships

| Module / System | Relationship |
| --- | --- |
| [`ai-core`](../ai-core/README.md) | Translates catalog functions to Anthropic tool-call representations. |
| [`agent-runtime`](../agent-runtime/README.md) | Can be composed by a caller where an Anthropic provider is selected. |

## Documentation

No module-specific design document is maintained for this integration.

## Deployment

> This module is not independently deployed.

No independent runtime owner or Deployment record applies to this integration.

## Development

- **Module Type:** `INTEGRATION`
- **Runtime:** `None`
- **Current PR Stack:** [mainline integration #10](https://github.com/TavallStudios/function-catalog/pull/10); documentation update: __PR_LINK__.
- Shared contribution policy: [Tavall Docs Git Workflow](https://github.com/TavallStudios/tavall-docs/blob/main/docs/quality/GIT_WORKFLOW.md).


<details>
<summary>Documentation Update State</summary>

### Current Locations

| Surface | Sync State | Location | Last Updated | Evidence |
| --- | --- | --- | --- | --- |
| GitHub | `PRIMARY` | `TavallStudios/function-catalog/claude-sdk/README.md` | 2026-09-27 12:51 PM PDT | __PR_URL__ |
| Notion | `NOT_APPLICABLE` | — | 2026-09-27 12:51 PM PDT | README routing surface; no 1:1 twin is assigned. |

### Update History

| Timestamp | Surface | Event | Location | Previous Location | Evidence | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| 2026-09-27 12:51 PM PDT | GitHub | `UPDATED` | `TavallStudios/function-catalog/claude-sdk/README.md` | `TavallStudios/function-catalog/claude-sdk/README.md` | __PR_URL__ | Canonicalized module ownership, runtime, and current PR routing. |

</details>

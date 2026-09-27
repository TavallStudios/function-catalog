# gemini-sdk

Owns Java Gemini 3 text/image client wrappers, response models, model/API enums, and response parsing helpers.

## Responsibility

### Owns
- Gemini API client integration and response types.
- Gemini-specific model/version selection and parsing helpers.

### Does Not Own
- Tavall's shared function catalog or provider-neutral agent execution.
- A long-lived application or model-hosting service.

## Repository Structure

function-catalog/
├── **[`gemini-sdk`](README.md) ← This Module**
├── [`ai-core`](../ai-core/README.md)
├── [`agent-runtime`](../agent-runtime/README.md)
├── [`codex-agent-provider`](../codex-agent-provider/README.md)
├── [`strands-agent-provider`](../strands-agent-provider/README.md)
├── [`openai-sdk`](../openai-sdk/README.md)
├── [`claude-sdk`](../claude-sdk/README.md)
└── [`mcp-server`](../mcp-server/README.md)

## Relationships

| Module / System | Relationship |
| --- | --- |
| [`ai-core`](../ai-core/README.md) | The Gemini client surface is separate from the shared catalog contract. |
| [`agent-runtime`](../agent-runtime/README.md) | A caller may compose provider integrations with the agent execution library. |

## Documentation

No module-specific design document is maintained for this integration.

## Deployment

> This module is not independently deployed.

No independent runtime owner or Deployment record applies to this integration.

## Development

- **Module Type:** `INTEGRATION`
- **Runtime:** `None`
- **Current PR Stack:** [mainline integration #10](https://github.com/TavallStudios/function-catalog/pull/10); documentation update: [PR #37](https://github.com/TavallStudios/function-catalog/pull/37).
- Shared contribution policy: [Tavall Docs Git Workflow](https://github.com/TavallStudios/tavall-docs/blob/main/docs/quality/GIT_WORKFLOW.md).


<details>
<summary>Documentation Update State</summary>

### Current Locations

| Surface | Sync State | Location | Last Updated | Evidence |
| --- | --- | --- | --- | --- |
| GitHub | `PRIMARY` | `TavallStudios/function-catalog/gemini-sdk/README.md` | 2026-09-27 12:59 PM PDT | [PR #37](https://github.com/TavallStudios/function-catalog/pull/37) |
| Notion | `NOT_APPLICABLE` | — | 2026-09-27 12:59 PM PDT | README routing surface; no 1:1 twin is assigned. |

### Update History

| Timestamp | Surface | Event | Location | Previous Location | Evidence | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| 2026-09-27 12:59 PM PDT | GitHub | `UPDATED` | `TavallStudios/function-catalog/gemini-sdk/README.md` | `TavallStudios/function-catalog/gemini-sdk/README.md` | [PR #37](https://github.com/TavallStudios/function-catalog/pull/37) | Canonicalized module ownership, runtime, and current PR routing. |

</details>

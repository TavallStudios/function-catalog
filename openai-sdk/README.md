# openai-sdk

Owns the OpenAI Java client wrapper and function-call/result adaptation used by callers.

## Responsibility

### Owns
- OpenAI client integration and tool/function-call translation.

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
├── **[`openai-sdk`](README.md) ← This Module**
├── [`claude-sdk`](../claude-sdk/README.md)
└── [`mcp-server`](../mcp-server/README.md)

## Relationships

| Module / System | Relationship |
| --- | --- |
| [`ai-core`](../ai-core/README.md) | Translates catalog functions to OpenAI tool-call representations. |
| [`agent-runtime`](../agent-runtime/README.md) | Can be composed by a caller where an OpenAI provider is selected. |

## Documentation

| Type | Document | Purpose | Surface |
| --- | --- | --- | --- |
| Progression | [Openai Sdk Progression](../docs/progression/OPENAI_SDK_PROGRESSION.md) | Module implementation, validation, and history. | GitHub |
| Progression | [Function Catalog System Progression](../docs/progression/FUNCTION_CATALOG_SYSTEM_PROGRESSION.md) | Cross-module architecture and system acceptance. | GitHub |
## Deployment

> This module is not independently deployed.

No independent runtime owner or Deployment record applies to this integration.

## Development

- **Module Type:** `INTEGRATION`
- **Runtime:** `None`
- **Current PR Stack:** [mainline integration #10](https://github.com/TavallStudios/function-catalog/pull/10); documentation update: [PR #37](https://github.com/TavallStudios/function-catalog/pull/37).
- Shared contribution policy: [Tavall Docs Git Workflow](https://github.com/TavallStudios/tavall-docs/blob/main/docs/quality/GIT_WORKFLOW.md).

- **Progression:** [Module Progression](../docs/progression/OPENAI_SDK_PROGRESSION.md) · [System Progression](../docs/progression/FUNCTION_CATALOG_SYSTEM_PROGRESSION.md).
- **Module CI:** Missing in audited main: `.tavallci/ci.yaml`.

<details>
<summary>Documentation Update State</summary>

### Current Locations

| Surface | Sync State | Location | Last Updated | Evidence |
| --- | --- | --- | --- | --- |
| GitHub | `PRIMARY` | `TavallStudios/function-catalog/openai-sdk/README.md` | 2026-09-27 5:59 PM PDT | [PR #37](https://github.com/TavallStudios/function-catalog/pull/37) |
| Notion | `NOT_APPLICABLE` | — | 2026-09-27 12:59 PM PDT | README routing surface; no 1:1 twin is assigned. |

### Update History

| Timestamp | Surface | Event | Location | Previous Location | Evidence | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| 2026-09-27 12:59 PM PDT | GitHub | `UPDATED` | `TavallStudios/function-catalog/openai-sdk/README.md` | `TavallStudios/function-catalog/openai-sdk/README.md` | [PR #37](https://github.com/TavallStudios/function-catalog/pull/37) | Canonicalized module ownership, runtime, and current PR routing. |
| 2026-09-27 5:59 PM PDT | GitHub | `UPDATED` | `TavallStudios/function-catalog/openai-sdk/README.md` | `TavallStudios/function-catalog/openai-sdk/README.md` | [PR #37](https://github.com/TavallStudios/function-catalog/pull/37) | Added module and System Progression routes and recorded module CI state. |

</details>

# Tavall Function Catalog

Tavall Function Catalog provides typed Java function definitions, scoped discovery and invocation, provider integrations, and an executable MCP server.

## Why Function Catalog

- Give Tavall operations a stable, machine-readable function contract.
- Keep catalog discovery and invocation on the same scoped view.
- Let MCP clients consume catalog functions through HTTP or stdio.

## Features

- Function, parameter, publication-schema, registration, and audit contracts.
- Provider-neutral agent execution boundaries and separate provider integrations.
- Gemini, OpenAI, Anthropic, Codex, and Strands modules.
- An executable MCP server with HTTP and stdio launch modes.

## Quick Start

This repository does not document published dependency coordinates. Use JDK 25 and the committed Gradle Wrapper to build and verify source:

```bash
./gradlew check
```

## How It Works

`ai-core` owns the typed function catalog. `agent-runtime` and provider modules add separate agent-execution capabilities. `mcp-server` exposes catalog functions through MCP. Applications select the modules they need.

## Project Structure

├── [`gemini-sdk`](gemini-sdk/README.md)
├── [`ai-core`](ai-core/README.md)
├── [`agent-runtime`](agent-runtime/README.md)
├── [`codex-agent-provider`](codex-agent-provider/README.md)
├── [`strands-agent-provider`](strands-agent-provider/README.md)
├── [`openai-sdk`](openai-sdk/README.md)
├── [`claude-sdk`](claude-sdk/README.md)
└── [`mcp-server`](mcp-server/README.md)

## Documentation

- [Function Catalog architecture](docs/AGENT_FUNCTION_CATALOG_ARCHITECTURE.md) — typed functions, scoped views, and boundaries.
- [Agent runtime architecture](docs/TAVALL_AGENT_RUNTIME_ARCHITECTURE.md) — provider-neutral execution responsibilities.
- [Repository workflow compatibility pointer](docs/quality/GIT_WORKFLOW.md) — redirects to shared policy.
- [Tavall Docs Git Workflow](https://github.com/TavallStudios/tavall-docs/blob/main/docs/quality/GIT_WORKFLOW.md) — shared contribution and review guidance.

## Requirements / Compatibility

- JDK 25 for the configured Gradle toolchain.
- Provider integrations require credentials and API access for the selected provider.
- MCP clients connect to a launched HTTP or stdio server.

## Building From Source

Run `./gradlew check`. The `mcp-server` module assembles the executable MCP application.

## Contributing

Open a pull request and follow the canonical Tavall contribution and review policy linked above.

## Deployment

The `mcp-server` module is independently executable. Current target and deployed source are not recorded in GitHub deployment evidence; see [the deployment record](TAVALL_FUNCTION_CATALOG_DEPLOYMENT.md).

## License

No license file is currently tracked. Contact the maintainers before redistributing or reusing this code.

<details>
<summary>Documentation Update State</summary>

### Current Locations

| Surface | Sync State | Location | Last Updated | Evidence |
| --- | --- | --- | --- | --- |
| GitHub | `PRIMARY` | `TavallStudios/function-catalog/README.md` | 2026-09-27 12:59 PM PDT | [PR #37](https://github.com/TavallStudios/function-catalog/pull/37) |
| Notion | `NOT_APPLICABLE` | — | 2026-09-27 12:59 PM PDT | README routing surface; no 1:1 twin is assigned. |

### Update History

| Timestamp | Surface | Event | Location | Previous Location | Evidence | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| 2026-09-27 12:59 PM PDT | GitHub | `CREATED` | `TavallStudios/function-catalog/README.md` | — | [PR #37](https://github.com/TavallStudios/function-catalog/pull/37) | Added a public project front door and source-backed module map. |

</details>

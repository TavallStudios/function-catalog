# Tavall agent runtime architecture

## Purpose

The `agent-runtime` module provides provider-neutral execution for Tavall custom AI agents. It deliberately does not own Tavall Cloud topology, machine placement, production authorization, browser transport, Codex process management, or repository workspaces.

The runtime turns an agent definition plus one job into a provider execution request containing only a metadata-only, fail-closed `AIFunctionCatalogView`.

```text
AIAgentDefinition + AIAgentJob
            |
            v
      AIAgentRuntime
            |
            +--> authoritative per-job Function Catalog view
            +--> role requested-function narrowing
            +--> pre-dispatch tool budget
            +--> timeout + view revocation
            |
            v
   AIFunctionCatalogView
            |
            v
      AIAgentProvider
```

## Provider neutrality

`AIAgentProvider` is the execution boundary. Codex, OpenAI, Claude, Gemini, or a future Tavall provider may implement it. No provider is structurally privileged and Codex is not a control-plane dependency.

Providers receive the agent definition, current job, runtime budget, and already-restricted Function Catalog view. They never receive the root catalog.

## Function scope

`AIAgentDefinition.requestedFunctionNames` is a role maximum, not authorization. `AIAgentFunctionViewResolver` supplies the authoritative per-job view and the runtime can only narrow it further.

The runtime verifies the policy view is backed by its exact root catalog, composes the role filter on the same view, and applies an invocation limit before provider execution. Derived views inherit parent budget/revocation controls, so a provider cannot widen the runtime-owned execution surface by deriving another view.

## Budgets and cancellation

`AIAgentExecutionBudget` carries wall-clock duration, tool-call count, and delegation limits.

Tool calls are counted by the Function Catalog view before dispatch instead of trusting provider-reported accounting. Provider execution runs in a dedicated virtual-thread task bounded by the configured timeout. The runtime revokes the concrete execution view whenever execution ends and cancels the provider task on timeout or interruption.

Provider implementations that launch external processes remain responsible for terminating those process trees when interrupted. Tavall Cloud independently owns OS/container/cgroup/network, CPU, memory, concurrency, workspace, and distributed-operation limits.

## Development-only placement

Custom Tavall agent execution is a development-node capability. This module is placement-agnostic, but Tavall Cloud must refuse to launch it on production, staging/release, database, ingress, proxy, or ordinary service nodes.

A development-hosted agent may still invoke separately authorized typed operations against other nodes through Tavall Cloud CONTROL. Execution placement and operation targets are independent.

## Multi-agent direction

Agent-to-agent work should become typed Tavall Cloud jobs, not provider-specific side channels. CONTROL can then select an eligible development worker and provider for every delegated task while preserving its own job, capability, resource, approval, and audit boundaries.

# Tavall agent Function Catalog architecture

## Decision

Tavall AI agents execute Tavall operations through the Function Catalog. The catalog is the canonical machine-operable API for function names, descriptions, JSON schemas, invocation policy, audit behavior, and structured results.

Agents are built on top of the catalog; they are not implemented inside it. Model/provider selection, planning, memory, task state, multi-agent orchestration, and distributed scheduling remain separate runtime concerns.

## Function views

An agent must not receive the entire catalog by default. Its runtime constructs a fail-closed `AIFunctionCatalogView` from the intersection of:

- agent role and declared tool requirements;
- job scope;
- environment;
- caller authorization;
- target-node capabilities;
- mutation and blast-radius policy.

The same view owns discovery and invocation. Hiding a function from model/tool discovery is not a security boundary by itself; a caller that already knows a hidden function name must still be denied at invocation time.

MCP publication uses the same view instead of maintaining a second tool registry or a publication-only filter. External MCP clients and first-party Tavall agents therefore share one capability boundary.

## Tavall Cloud boundary

Tavall Cloud remains the distributed execution authority.

Every Tavall server may run the lightweight Tavall Cloud node agent. **Custom Tavall AI agents, model runtimes, Codex workers, browser automation, and broad development workspaces are development-node capabilities only.** Production, staging/release, database, ingress, proxy, and ordinary service nodes must not host custom agent/model execution merely because the Tavall node agent is installed.

Production nodes may still expose bounded typed operational functions to CONTROL. A custom agent running on an explicitly classified development node may call an authorized Function Catalog operation that CONTROL routes to another target node. This keeps model/runtime overhead and autonomous execution off production infrastructure while preserving typed remote inspection and tightly scoped operations.

Agent execution placement and operation targets are therefore independent, with an additional hard placement invariant:

```text
custom Tavall agent execution node -> DEVELOPMENT only
operation target                 -> policy-selected node/service
```

Conceptually:

```text
Tavall AI Agent (development node only)
    -> AIFunctionCatalogView
    -> Function Catalog
    -> Tavall Cloud CONTROL routing/policy/audit
    -> target node capability
```

The Function Catalog must not learn Tavall Cloud topology, SSH details, node addresses, environment classification, or provider-specific routing. Tavall Cloud owns those concerns and must fail closed if a requested agent execution node is not development-classified.

## Codex and other agents

Codex is an execution option, not the architecture. Agent runtimes should be able to delegate work through typed agent/task functions so Tavall Cloud can select Codex, another Tavall model agent, or a future provider from eligible **development nodes only**.

Broad shell access is not a normal catalog primitive. Development execution belongs in an explicitly authorized disposable workspace or sandbox. Production operations should prefer bounded typed functions.

## Blast radius

Follow-up Tavall Cloud work should model job and function authorization explicitly rather than relying on prompts. At minimum the policy input should cover:

- execution node, which must be development-classified for custom agents;
- target environment/node set;
- read versus mutation scope;
- affected repository/service/data scope;
- risk class;
- CPU, memory, concurrency, and timeout budgets;
- approval requirements;
- audit identity and originating agent/job.

High-risk architecture domains such as Tavall DI, CONTROL authentication, production manifests, and database topology should be eligible for broad experimentation only in disposable development environments, with stronger validation required before promotion.

## Initial implementation in this PR

This PR introduces the first reusable enforcement primitive:

- `AIFunctionCatalogView` filters function discovery;
- invocation through the view fails closed for hidden functions;
- MCP publication can consume the same invocation-capable view;
- the existing predicate MCP API remains source-compatible but is implemented through a catalog view.

The next Tavall Cloud implementation can build development-only agent placement, node capability advertisement, agent job policy, and distributed routing on top of this boundary without creating a second tool system.

# Tavall Cloud Function Contracts

This module owns stable Function Catalog names and input schemas for Tavall Cloud development execution. It does not own Tavall Cloud topology, provider selection, credentials, authorization, job state, or storage.

Tavall Cloud supplies an adapter that implements `TavallCloudDeveloperFunctions` and delegates every invocation to DEVELOPMENT CONTROL.

## Published contract

- `cloud_dev_ci_start`
- `cloud_dev_ci_inspect`
- `cloud_dev_ci_cancel`
- `cloud_dev_ci_evidence`
- `cloud_dev_tool_exec`
- `cloud_dev_github_exec`

The CI contract is exact-head and origin-aware. Supported origins are GitHub Bot, ChatGPT, Codex, manual execution, and scheduler execution.

`cloud_dev_tool_exec` is intentionally stronger than ordinary typed CI. `SHELL` is a trusted development-sandbox capability and must be excluded from normal automated/webhook function views unless policy explicitly grants it. Prefer typed tool classes such as Gradle, Maven, npm, Java, Docker, tests, servers, and bot/E2E harnesses.

`cloud_dev_github_exec` is for bounded GitHub operations in an authorized leased workspace. Higher-level PR/review functions remain preferable when they exist.

## Execution boundary

```text
Function Catalog contract
        -> Tavall Cloud adapter
        -> DEVELOPMENT CONTROL policy/audit
        -> durable developer job / sandbox operation
        -> selected execution provider
        -> DEV STORAGE evidence
```

The Function Catalog must not learn node addresses, SSH details, GitHub installation secrets, sandbox credentials, or storage topology.

## CI convention

The Cloud contracts module is built as part of the Function Catalog Gradle project. The repository-owned `.tavallci/ci.yaml` selects the default checks, and Tavall CI resolves the exact source and runs them on a Tavall Cloud Executor. GitHub Actions are not an execution path. Public package publication is an explicit optional Tavall CI profile; package hosting does not schedule builds.

## Current metadata note

The catalog resolves `@AIFunction` metadata declared on implemented interfaces. Parameter names and Java types remain stable because the repository compiles with `-parameters`. Parameter-level annotation descriptions are currently resolved from the concrete invokable method, so adapters that need those descriptions before catalog inheritance is enhanced should mirror `@AIParam` descriptions on their implementation methods.

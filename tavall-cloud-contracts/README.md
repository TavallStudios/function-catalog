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

## Local CI convention

This repository uses the same Tavall repository-owned CI entrypoint expected by multi-origin runners:

```text
bash scripts/ci/run ...
        -> bash scripts/ci/verify <profile>
```

GitHub Actions workflows are intentionally not part of the execution architecture. Build, integration, and publish work runs on Tavall/local infrastructure and GitHub is used as source control, review, package hosting, and result reporting.

## Current metadata note

The catalog resolves `@AIFunction` metadata declared on implemented interfaces. Parameter names and Java types remain stable because the repository compiles with `-parameters`. Parameter-level annotation descriptions are currently resolved from the concrete invokable method, so adapters that need those descriptions before catalog inheritance is enhanced should mirror `@AIParam` descriptions on their implementation methods.

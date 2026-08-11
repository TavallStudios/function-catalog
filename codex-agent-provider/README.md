# Codex Agent Provider

Provider adapter for delegating implementation work to the Codex CLI inside an already-authorized development workspace lease.

The provider is intentionally narrow:

- it implements `AIAgentProvider` with provider id `codex`;
- the caller supplies an absolute workspace that must resolve to the root of an explicit trusted Git repository through `CodexWorkspaceResolver`;
- the caller must also supply a `CodexProcessIsolationSupervisor`; there is deliberately no default direct `ProcessBuilder` execution path;
- the supervisor is owned by the Tavall Cloud development worker and must establish the isolated execution identity plus process group/cgroup from launch, prevent Codex and its descendants from reading the worker JVM process environment (including through `/proc`), bound stdout/stderr while running, and terminate or prove quiescent the complete owned process group before returning;
- Codex runs with `exec`, an explicit `read-only` or `workspace-write` sandbox, `approval_policy="never"`, ephemeral sessions, ignored user config, JSONL events, and prompt input redirected from a provider-owned temporary file;
- no dangerous sandbox/approval bypass flag is supported;
- the environment supplied to the supervisor is reduced to a small runtime/OpenAI authentication allowlist, so Tavall CONTROL/database/service credentials are not intentionally delegated to Codex;
- the provider rejects supervisor output that exceeds its bounded capture contract;
- the final-message file is read from a bounded tail;
- provider-created prompt/result files live in a temporary directory inside the workspace and are removed after execution;
- the provider does not expose the root Function Catalog or Tavall CONTROL to Codex.

This module is a development-workspace implementation worker, not a second Tavall control plane. Tavall Cloud owns development-only placement, workspace leases, process/user/cgroup/network isolation, job authority, and any future remote tool bridge. A host that cannot provide the required process supervisor cannot construct a runnable Codex provider.

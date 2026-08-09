# Codex Agent Provider

Provider adapter for delegating implementation work to the Codex CLI inside an already-authorized development workspace lease.

The provider is intentionally narrow:

- it implements `AIAgentProvider` with provider id `codex`;
- the caller supplies an absolute workspace that must resolve to the root of an explicit trusted Git repository through `CodexWorkspaceResolver`;
- Codex runs with `exec`, an explicit `read-only` or `workspace-write` sandbox, `approval_policy="never"`, ephemeral sessions, ignored user config, JSONL events, and prompt input redirected from a provider-owned temporary file;
- no dangerous sandbox/approval bypass flag is supported;
- inherited process environment is replaced with a small runtime/OpenAI authentication allowlist, so Tavall CONTROL/database/service credentials are not inherited by Codex;
- stdout and stderr are drained concurrently into bounded in-memory tail buffers instead of unbounded log files;
- the final-message file is read from a bounded tail;
- provider-created prompt/result files live in a temporary directory inside the workspace and are removed after execution;
- process interruption terminates the observed Codex process tree before the workspace is released;
- the provider does not expose the root Function Catalog or Tavall CONTROL to Codex.

This module is a development-workspace implementation worker, not a second Tavall control plane. Tavall Cloud owns development-only placement, workspace leases, outer process/cgroup/network isolation, job authority, and any future remote tool bridge.

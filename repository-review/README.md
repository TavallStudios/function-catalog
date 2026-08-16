# Repository Review

`repository-review` exposes GitHub repository state and review publication as typed Function Catalog functions. It deliberately contains no LLM review policy or analyzer intelligence; `tavall-agent-review` owns that layer.

## Functions

- `github_inspect_pr` resolves a pull request to an exact head SHA and returns its base/head refs, unified diff, and changed-file list.
- `github_inspect_ref_range` resolves arbitrary base/head refs to exact SHAs before returning the diff.
- `github_list_review_threads` returns existing inline review comments so review agents can reconcile stable finding fingerprints instead of duplicating comments.
- `github_review_pr` publishes a review only after re-reading the pull request and proving that its current head still equals the reviewed `exactHeadSha`.

## GitHub configuration

The registrar uses the same fail-closed environment contract as repository staging:

- `FUNCTION_CATALOG_GITHUB_TOKEN`
- `FUNCTION_CATALOG_GITHUB_REPOSITORIES` as a comma-separated explicit `owner/repo` allowlist
- `FUNCTION_CATALOG_GITHUB_API_URL` optionally overrides the GitHub API base URL

To expose the functions through the MCP server, include `org.tavall.ai.review.RepositoryReviewRegistrar` as a registrar class. Keep repository execution out of this transport module; untrusted build/test execution belongs in the Tavall Cloud sandbox runtime.

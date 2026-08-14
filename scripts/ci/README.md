# Function Catalog local workflows

Repository-local commands are the source of truth for verification and publishing.

## Verify

```bash
scripts/ci/verify
```

This runs the same acceptance unit formerly duplicated in GitHub Actions: Java 25 Gradle `clean check`, local Maven publication, and staged MCP runtime assembly.

## Publish

```bash
scripts/release/publish [x.y.z]
```

If no version is supplied, the script reads `gradle/release-version.txt`. Publishing validates semantic version syntax and requires explicitly supplied `GITHUB_TOKEN` and `GITHUB_ACTOR` credentials before running `clean check publish`.

Remote automation may execute these local commands and report status, but it must not maintain a second build/publish graph in workflow YAML.

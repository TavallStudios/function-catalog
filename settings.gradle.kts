plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "function-catalog"
include(
    "gemini-sdk",
    "ai-core",
    "repository-staging",
    "repository-review",
    "agent-runtime",
    "codex-agent-provider",
    "openai-sdk",
    "claude-sdk",
    "mcp-server"
)
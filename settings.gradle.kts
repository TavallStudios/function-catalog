plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "function-catalog"
include("gemini-sdk", "ai-core", "agent-runtime", "openai-sdk", "claude-sdk", "mcp-server")

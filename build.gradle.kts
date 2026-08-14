import java.util.zip.ZipFile

plugins {
    base
}

group = "org.tavall"
extra["versionTagPrefix"] = "function-catalog"
apply(from = "gradle/git-version.gradle.kts")
version = extra["gitVersion"] as String

val assertj = libs.assertj
val classgraph = libs.classgraph
val googleGenai = libs.google.genai
val jackson = libs.jackson
val junit = libs.junit
val junitLauncher = libs.junit.launcher
val mcpCore = libs.mcp.core
val mcpJackson = libs.mcp.jackson
val mockitoCore = libs.mockito.core
val mockitoJunit = libs.mockito.junit
val slf4j = libs.slf4j

subprojects {
    group = rootProject.group
    version = rootProject.version
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")

    extensions.configure<JavaPluginExtension> {
        toolchain.languageVersion = JavaLanguageVersion.of(25)
        withSourcesJar()
        withJavadocJar()
    }
    repositories { mavenCentral() }
    dependencyLocking { lockAllConfigurations() }
    tasks.withType<JavaCompile>().configureEach { options.compilerArgs.add("-parameters") }
    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        maxHeapSize = "256m"
        jvmArgs("-XX:MaxMetaspaceSize=256m")
    }
    tasks.withType<Jar>().configureEach {
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
    }
    val verifyJarContents = tasks.register("verifyJarContents") {
        dependsOn(tasks.named("jar"))
        val archive = tasks.named<Jar>("jar").flatMap { it.archiveFile }
        inputs.file(archive)
        doLast {
            val forbidden = listOf("com/fasterxml/", "com/google/genai/", "io/modelcontextprotocol/", "org/slf4j/")
            ZipFile(archive.get().asFile).use { jar ->
                val embedded = jar.entries().asSequence().map { it.name }
                    .firstOrNull { entry -> forbidden.any(entry::startsWith) }
                check(embedded == null) { "Third-party class embedded in first-party JAR: $embedded" }
            }
        }
    }
    tasks.named("check") { dependsOn(verifyJarContents) }
    extensions.configure<PublishingExtension> {
        publications {
            create<MavenPublication>("mavenJava") {
                from(components["java"])
                artifactId = project.name
            }
        }
        repositories {
            val token = providers.environmentVariable("GITHUB_TOKEN")
            if (token.isPresent) {
                maven {
                    name = "GitHubPackages"
                    url = uri("https://maven.pkg.github.com/TavallStudios/function-catalog")
                    credentials {
                        username = providers.environmentVariable("GITHUB_ACTOR").orNull
                        password = token.get()
                    }
                }
            }
        }
    }
}

project(":ai-core") {
    dependencies {
        "api"(jackson)
        "api"(slf4j)
        "api"(classgraph)
        "testImplementation"(junit)
        "testImplementation"(assertj)
        "testRuntimeOnly"(junitLauncher)
    }
}

project(":repository-staging") {
    dependencies {
        "api"(project(":ai-core"))
        "api"(jackson)
        "api"(slf4j)
        "testImplementation"(junit)
        "testImplementation"(assertj)
        "testRuntimeOnly"(junitLauncher)
    }
}

project(":agent-runtime") {
    dependencies {
        "api"(project(":ai-core"))
        "api"(jackson)
        "api"(slf4j)
        "testImplementation"(junit)
        "testImplementation"(assertj)
        "testRuntimeOnly"(junitLauncher)
    }
}

project(":codex-agent-provider") {
    dependencies {
        "api"(project(":agent-runtime"))
        "api"(jackson)
        "api"(slf4j)
        "testImplementation"(junit)
        "testImplementation"(assertj)
        "testRuntimeOnly"(junitLauncher)
    }
}

for (module in listOf(":openai-sdk", ":claude-sdk")) {
    project(module) {
        dependencies {
            "api"(project(":ai-core"))
            "api"(jackson)
            "api"(slf4j)
            "testImplementation"(junit)
            "testRuntimeOnly"(junitLauncher)
        }
    }
}

project(":gemini-sdk") {
    dependencies {
        "api"(googleGenai)
        "testImplementation"(junit)
        "testImplementation"(assertj)
        "testImplementation"(mockitoCore)
        "testImplementation"(mockitoJunit)
        "testRuntimeOnly"(junitLauncher)
    }
}

project(":mcp-server") {
    apply(plugin = "application")
    extensions.configure<JavaApplication> {
        mainClass = "org.tavall.ai.mcp.server.AIFunctionMcpServerLauncher"
    }
    dependencies {
        "api"(project(":ai-core"))
        "runtimeOnly"(project(":repository-staging"))
        "testImplementation"(project(":repository-staging"))
        "api"(jackson)
        "api"(slf4j)
        "api"(mcpCore)
        "api"(mcpJackson)
        "testImplementation"(junit)
        "testImplementation"(assertj)
        "testRuntimeOnly"(junitLauncher)
    }
    tasks.named<Test>("test") {
        useJUnitPlatform { excludeTags("integration") }
    }
    val testSourceSet = extensions.getByType<SourceSetContainer>().named("test")
    tasks.register<Test>("integrationTest") {
        description = "Runs MCP tests that require an authenticated Codex executable."
        group = LifecycleBasePlugin.VERIFICATION_GROUP
        testClassesDirs = testSourceSet.get().output.classesDirs
        classpath = testSourceSet.get().runtimeClasspath
        useJUnitPlatform { includeTags("integration") }
        shouldRunAfter(tasks.named("test"))
    }
}

val stageRuntime = tasks.register<Sync>("stageRuntime") {
    dependsOn(":mcp-server:jar")
    into(layout.projectDirectory.dir("distribution"))
    from(project(":mcp-server").tasks.named<Jar>("jar").flatMap { it.archiveFile }) {
        rename { "application.jar" }
    }
    into("libs") {
        from(project(":mcp-server").configurations.named("runtimeClasspath"))
    }
}

tasks.named("assemble") { dependsOn(stageRuntime) }

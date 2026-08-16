# Minecraft WorldOps Function Catalog Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a canonical typed `minecraft-worldops` Function Catalog domain whose Java functions automatically project to MCP and whose provider boundary can execute against a host-authorized Mineflayer/FAWE runtime without exposing raw WorldEdit commands.

**Architecture:** Follow the existing Function Catalog `Functions -> Service -> Provider` pattern. Typed requests/results and semantic validation live in Function Catalog; external Minecraft I/O lives behind `MinecraftWorldOpsProvider`. The Project Novus Mineflayer implementation is connected by an authorized runtime/host adapter, not copied into Function Catalog.

**Tech Stack:** Java 25, Gradle, Function Catalog `ai-core` annotations/catalog/MCP publisher, Tavall Java Tools baseline, JUnit 5.

## Global Constraints

- Base is `working/java-tools-platform-adoption` / Function Catalog PR #15.
- No raw `minecraft_world_command`, `worldedit_command`, chat-command, shell, RCON, credential, server-address, or arbitrary WorldEdit-pattern function.
- Function names are canonical snake_case `minecraft_world_*` names.
- Logical world references are always checked by the host-scoped provider; naming a world never grants authority.
- No production code before the corresponding RED test is executed and fails for the expected reason.
- MCP projection must come from canonical annotated Java functions, not a second hand-written schema copy.
- Run repository-owned local verification only; do not use GitHub Actions.

---

## Task 1: Lock the initial callable surface with a RED catalog test

**Files:**
- Create: `mcp-server/src/test/java/org/tavall/ai/minecraft/worldops/MinecraftWorldOpsCatalogContractTest.java`

- [ ] Add a reflection/catalog test that expects exactly these initial function names:
  - `minecraft_world_block_set`
  - `minecraft_world_region_set`
  - `minecraft_world_region_walls`
  - `minecraft_world_region_replace`
  - `minecraft_world_region_clear`
  - `minecraft_world_clipboard_copy`
  - `minecraft_world_clipboard_cut`
  - `minecraft_world_clipboard_paste`
  - `minecraft_world_clipboard_rotate`
  - `minecraft_world_clipboard_flip`
  - `minecraft_world_schematic_load`
  - `minecraft_world_schematic_save`
  - `minecraft_world_history_undo`
  - `minecraft_world_history_redo`
- [ ] Assert the function set contains no generic command/chat/shell function.
- [ ] Run the focused test and verify RED because the WorldOps domain is absent.
- [ ] Commit the RED contract test independently.

## Task 2: Add typed Minecraft world primitives

**Files:**
- Create: `minecraft-worldops/src/main/java/org/tavall/ai/minecraft/worldops/MinecraftBlockPosition.java`
- Create: `minecraft-worldops/src/main/java/org/tavall/ai/minecraft/worldops/MinecraftBlockRegion.java`
- Create: `minecraft-worldops/src/main/java/org/tavall/ai/minecraft/worldops/MinecraftBlockState.java`
- Create: `minecraft-worldops/src/main/java/org/tavall/ai/minecraft/worldops/MinecraftWorldRef.java`
- Create: `minecraft-worldops/src/main/java/org/tavall/ai/minecraft/worldops/MinecraftClipboardRotation.java`
- Create: `minecraft-worldops/src/main/java/org/tavall/ai/minecraft/worldops/MinecraftFlipDirection.java`
- Create: `minecraft-worldops/src/main/java/org/tavall/ai/minecraft/worldops/MinecraftWorldOpsResult.java`
- Create: matching focused tests under `minecraft-worldops/src/test/java/...`

- [ ] Add RED tests for integer positions, normalized regions, safe Minecraft block ids/states, safe schematic ids, supported rotations, and supported flip directions.
- [ ] Run focused tests and verify RED.
- [ ] Implement immutable validated value types.
- [ ] Reject newline/control characters, traversal-like schematic names, invalid block ids, reversed regions, and unsupported enum values before provider I/O.
- [ ] Run focused tests to GREEN and commit.

## Task 3: Add typed operation requests and provider boundary

**Files:**
- Create: operation request records under `minecraft-worldops/src/main/java/org/tavall/ai/minecraft/worldops/`
- Create: `MinecraftWorldOpsProvider.java`
- Create: provider contract tests

- [ ] Add RED tests proving every operation reaches the provider as typed data, not a constructed raw WorldEdit command.
- [ ] Define request records for block, region, clipboard, schematic, and history operations.
- [ ] Keep credentials, Minecraft host/port, operator tokens, RCON credentials, and shell/process fields out of requests.
- [ ] Define `MinecraftWorldOpsProvider` with typed methods only; do not add `execute(String)` or equivalent generic escape hatches.
- [ ] Require the provider implementation to reject world refs outside its host-authorized scope.
- [ ] Run focused tests to GREEN and commit.

## Task 4: Implement semantic service and annotated functions

**Files:**
- Create: `minecraft-worldops/src/main/java/org/tavall/ai/minecraft/worldops/MinecraftWorldOpsService.java`
- Create: `minecraft-worldops/src/main/java/org/tavall/ai/minecraft/worldops/MinecraftWorldOpsFunctions.java`
- Create: service/function tests

- [ ] Add RED tests for service validation, provider error propagation, and result mapping.
- [ ] Implement service methods that normalize/validate requests and delegate to the typed provider.
- [ ] Implement the fourteen `@AIFunction` methods with one typed request parameter each.
- [ ] Keep descriptions semantic (`replace blocks in this region`) rather than FAWE-specific (`run //replace`).
- [ ] Run focused tests to GREEN and commit.

## Task 5: Register the domain and prove automatic MCP projection

**Files:**
- Modify: `settings.gradle.kts`
- Modify: root/module Gradle configuration as required
- Create: `minecraft-worldops/src/main/java/org/tavall/ai/minecraft/worldops/MinecraftWorldOpsRegistrar.java`
- Modify: `mcp-server` dependency/profile wiring
- Modify: `MinecraftWorldOpsCatalogContractTest.java`

- [ ] Add a RED MCP projection test that registers a fake provider and inspects the generated schemas for the fourteen functions.
- [ ] Assert no separately hand-authored MCP schema is needed.
- [ ] Add `minecraft-worldops` to the active Gradle graph and `mcp-server` test/runtime projection path.
- [ ] Implement registrar wiring following current Function Catalog registrar conventions.
- [ ] Run the catalog/MCP tests to GREEN and commit.

## Task 6: Prepare host integration without embedding Project Novus

**Files:**
- Create: narrow provider adapter SPI/configuration documentation under `minecraft-worldops/src/main/...` or module docs
- Modify: tests as needed

- [ ] Prove with tests that provider construction is external/host-scoped and Function Catalog can run entirely with a fake provider.
- [ ] Do not add a dependency on Project Novus, Mineflayer, FAWE, Paper, RCON, or Minecraft protocol libraries.
- [ ] Document that Tavall AI/host supplies an authorized provider backed by the Project Novus WorldOps executor.
- [ ] Commit the boundary documentation/tests.

## Task 7: Exact-head validation and disposable runtime acceptance

- [ ] Run the repository-owned Java 25 local verifier on the exact head.
- [ ] Run the explicit Minecraft WorldOps MCP/catalog profile with a fake provider and validate generated schemas.
- [ ] Connect the validated catalog functions through the authorized runtime adapter to the Project Novus Mineflayer executor in a disposable Paper + FAWE environment.
- [ ] Exercise all fourteen initial functions and prove unauthorized world refs fail closed.
- [ ] Reconcile the Draft PR body with exact head, Java 25 evidence, Tavall AI consumer PR, Project Novus executor PR, and remaining live acceptance.
- [ ] Keep Draft until cross-repository acceptance is truthful.
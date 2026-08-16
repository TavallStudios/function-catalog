# Minecraft WorldOps Function Catalog Implementation Plan

**Goal:** Add a canonical typed `minecraft-worldops` Function Catalog domain whose Java functions automatically project to MCP and whose provider boundary can execute against a host-authorized Mineflayer/FAWE runtime without exposing raw WorldEdit commands.

**Architecture:** Follow `Functions -> Service -> Provider`. Typed requests/results and semantic validation live in Function Catalog; external Minecraft I/O lives behind `MinecraftWorldOpsProvider`. Project Novus is connected by an authorized runtime/host adapter, not imported here.

## Testing discipline

Follow Tavall delegate-style testing.

- Build the real concrete WorldOps boundary and test that boundary directly.
- Use real request/value/service/function/registrar objects.
- Fake only the true external `MinecraftWorldOpsProvider` boundary when a live Minecraft runtime is not the boundary under test.
- Java tests match production class names plus `Test` and mirror production packages.
- Code, tests, registration, and direct docs move together as coherent system boundaries.
- Do not use reflection merely to prove a future class is missing.
- Do not create separate RED-only contract commits.
- Run repository-owned local verification only; never GitHub Actions.

## Task 1: Add typed Minecraft world primitives

Implement:

- `MinecraftBlockPosition`
- `MinecraftBlockRegion`
- `MinecraftBlockState`
- `MinecraftWorldRef`
- `MinecraftClipboardRotation`
- `MinecraftFlipDirection`
- `MinecraftWorldOpsResult`

Matching tests such as `MinecraftBlockRegionTest` exercise the real value types, including normalization/rejection behavior for invalid coordinates, block ids/states, schematic identifiers, rotations, and flip directions.

## Task 2: Add typed operation requests and provider boundary

Implement typed request records for block, region, clipboard, schematic, and history operations plus `MinecraftWorldOpsProvider`.

The provider exposes typed methods only. It must not contain `execute(String)`, raw command execution, RCON credentials, shell/process arguments, or model-selectable host credentials.

Provider-boundary tests call the real service later with a narrow fake provider and realistic requests.

## Task 3: Implement the semantic service

Implement `MinecraftWorldOpsService` with the full initial surface:

- block set;
- region set/walls/replace/clear;
- clipboard copy/cut/paste/rotate/flip;
- schematic load/save;
- history undo/redo.

`MinecraftWorldOpsServiceTest` instantiates the real service with a fake external provider and verifies typed delegation, validation, provider failure propagation, and unauthorized/unscoped target behavior.

## Task 4: Implement annotated canonical functions

Implement `MinecraftWorldOpsFunctions` with exactly these canonical names:

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

`MinecraftWorldOpsFunctionsTest` calls the real functions against the real service and fake external provider. It verifies semantic behavior and the actual annotations/names without testing a nonexistent class.

There is no generic `minecraft_world_command`, `worldedit_command`, chat-command, shell, or arbitrary command function.

## Task 5: Register and project to MCP

Implement `MinecraftWorldOpsRegistrar` and required Gradle/module wiring.

`MinecraftWorldOpsRegistrarTest` registers the real WorldOps functions into the real catalog using a fake external provider. The MCP projection test then inspects schemas projected from those real registered functions. Do not hand-author a duplicate MCP schema.

## Task 6: Preserve the host boundary

Function Catalog must remain runnable with a fake provider and must not depend on Project Novus, Mineflayer, FAWE, Paper, RCON, or Minecraft protocol libraries.

Document the authorized runtime/host adapter boundary that connects the provider to Project Novus.

## Task 7: Exact-head validation and disposable acceptance

- Run the repository-owned Java 25 local verifier.
- Run the real Minecraft WorldOps catalog/MCP profile with a fake provider.
- Connect the same real functions through the authorized host adapter to Project Novus in disposable Paper + FAWE.
- Exercise all fourteen functions and unauthorized-world rejection.
- Record exactly what ran and remaining gaps.
- Keep Draft until cross-repository acceptance is truthful.
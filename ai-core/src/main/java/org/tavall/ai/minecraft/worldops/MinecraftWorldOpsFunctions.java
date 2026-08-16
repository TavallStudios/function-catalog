package org.tavall.ai.minecraft.worldops;

import org.tavall.ai.core.annotation.AIFunction;
import org.tavall.ai.core.annotation.AIParam;

import java.util.Objects;

public final class MinecraftWorldOpsFunctions {
    private final MinecraftWorldOpsService service;

    public MinecraftWorldOpsFunctions(MinecraftWorldOpsService service) {
        this.service = Objects.requireNonNull(service, "service");
    }

    @AIFunction(name = "minecraft_world_block_set", description = "Set one block in an authorized Minecraft world")
    public MinecraftWorldOpsResult setBlock(@AIParam(name = "request") MinecraftBlockSetRequest request) { return service.setBlock(request); }

    @AIFunction(name = "minecraft_world_region_set", description = "Set every block in an authorized Minecraft region")
    public MinecraftWorldOpsResult setRegion(@AIParam(name = "request") MinecraftRegionSetRequest request) { return service.setRegion(request); }

    @AIFunction(name = "minecraft_world_region_walls", description = "Set the walls of an authorized Minecraft region")
    public MinecraftWorldOpsResult walls(@AIParam(name = "request") MinecraftRegionSetRequest request) { return service.walls(request); }

    @AIFunction(name = "minecraft_world_region_replace", description = "Replace one block state with another inside an authorized Minecraft region")
    public MinecraftWorldOpsResult replace(@AIParam(name = "request") MinecraftRegionReplaceRequest request) { return service.replace(request); }

    @AIFunction(name = "minecraft_world_region_clear", description = "Clear an authorized Minecraft region")
    public MinecraftWorldOpsResult clear(@AIParam(name = "request") MinecraftSelectionRequest request) { return service.clear(request); }

    @AIFunction(name = "minecraft_world_clipboard_copy", description = "Copy an authorized Minecraft region into the host clipboard")
    public MinecraftWorldOpsResult copy(@AIParam(name = "request") MinecraftSelectionRequest request) { return service.copy(request); }

    @AIFunction(name = "minecraft_world_clipboard_cut", description = "Cut an authorized Minecraft region into the host clipboard")
    public MinecraftWorldOpsResult cut(@AIParam(name = "request") MinecraftSelectionRequest request) { return service.cut(request); }

    @AIFunction(name = "minecraft_world_clipboard_paste", description = "Paste the host clipboard into an authorized Minecraft world")
    public MinecraftWorldOpsResult paste(@AIParam(name = "request") MinecraftClipboardPasteRequest request) { return service.paste(request); }

    @AIFunction(name = "minecraft_world_clipboard_rotate", description = "Rotate the host clipboard for an authorized Minecraft world")
    public MinecraftWorldOpsResult rotate(
            @AIParam(name = "world") MinecraftWorldRef world,
            @AIParam(name = "rotation") MinecraftClipboardRotation rotation) { return service.rotate(world, rotation); }

    @AIFunction(name = "minecraft_world_clipboard_flip", description = "Flip the host clipboard for an authorized Minecraft world")
    public MinecraftWorldOpsResult flip(
            @AIParam(name = "world") MinecraftWorldRef world,
            @AIParam(name = "direction") MinecraftFlipDirection direction) { return service.flip(world, direction); }

    @AIFunction(name = "minecraft_world_schematic_load", description = "Load a validated schematic into the authorized host clipboard")
    public MinecraftWorldOpsResult loadSchematic(@AIParam(name = "request") MinecraftSchematicRequest request) { return service.loadSchematic(request); }

    @AIFunction(name = "minecraft_world_schematic_save", description = "Save the authorized host clipboard as a validated schematic")
    public MinecraftWorldOpsResult saveSchematic(@AIParam(name = "request") MinecraftSchematicRequest request) { return service.saveSchematic(request); }

    @AIFunction(name = "minecraft_world_history_undo", description = "Undo the last authorized WorldOps mutation")
    public MinecraftWorldOpsResult undo(@AIParam(name = "world") MinecraftWorldRef world) { return service.undo(world); }

    @AIFunction(name = "minecraft_world_history_redo", description = "Redo the last authorized WorldOps mutation")
    public MinecraftWorldOpsResult redo(@AIParam(name = "world") MinecraftWorldRef world) { return service.redo(world); }
}
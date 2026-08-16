package org.tavall.ai.minecraft.worldops;

public interface MinecraftWorldOpsProvider {
    MinecraftWorldOpsResult setBlock(MinecraftBlockSetRequest request);
    MinecraftWorldOpsResult setRegion(MinecraftRegionSetRequest request);
    MinecraftWorldOpsResult walls(MinecraftRegionSetRequest request);
    MinecraftWorldOpsResult replace(MinecraftRegionReplaceRequest request);
    MinecraftWorldOpsResult clear(MinecraftSelectionRequest request);
    MinecraftWorldOpsResult copy(MinecraftSelectionRequest request);
    MinecraftWorldOpsResult cut(MinecraftSelectionRequest request);
    MinecraftWorldOpsResult paste(MinecraftClipboardPasteRequest request);
    MinecraftWorldOpsResult rotate(MinecraftWorldRef world, MinecraftClipboardRotation rotation);
    MinecraftWorldOpsResult flip(MinecraftWorldRef world, MinecraftFlipDirection direction);
    MinecraftWorldOpsResult loadSchematic(MinecraftSchematicRequest request);
    MinecraftWorldOpsResult saveSchematic(MinecraftSchematicRequest request);
    MinecraftWorldOpsResult undo(MinecraftWorldRef world);
    MinecraftWorldOpsResult redo(MinecraftWorldRef world);
}
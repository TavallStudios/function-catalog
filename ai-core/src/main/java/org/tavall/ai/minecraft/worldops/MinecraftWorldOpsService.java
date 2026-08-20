package org.tavall.ai.minecraft.worldops;

import java.util.Objects;

public final class MinecraftWorldOpsService {
    private final MinecraftWorldOpsProvider provider;

    public MinecraftWorldOpsService(MinecraftWorldOpsProvider provider) {
        this.provider = Objects.requireNonNull(provider, "provider");
    }

    public MinecraftWorldOpsResult setBlock(MinecraftBlockSetRequest request) { return provider.setBlock(require(request)); }
    public MinecraftWorldOpsResult setRegion(MinecraftRegionSetRequest request) { return provider.setRegion(require(request)); }
    public MinecraftWorldOpsResult walls(MinecraftRegionSetRequest request) { return provider.walls(require(request)); }
    public MinecraftWorldOpsResult replace(MinecraftRegionReplaceRequest request) { return provider.replace(require(request)); }
    public MinecraftWorldOpsResult clear(MinecraftSelectionRequest request) { return provider.clear(require(request)); }
    public MinecraftWorldOpsResult copy(MinecraftSelectionRequest request) { return provider.copy(require(request)); }
    public MinecraftWorldOpsResult cut(MinecraftSelectionRequest request) { return provider.cut(require(request)); }
    public MinecraftWorldOpsResult paste(MinecraftClipboardPasteRequest request) { return provider.paste(require(request)); }
    public MinecraftWorldOpsResult rotate(MinecraftWorldRef world, MinecraftClipboardRotation rotation) {
        return provider.rotate(require(world), require(rotation));
    }
    public MinecraftWorldOpsResult flip(MinecraftWorldRef world, MinecraftFlipDirection direction) {
        return provider.flip(require(world), require(direction));
    }
    public MinecraftWorldOpsResult loadSchematic(MinecraftSchematicRequest request) { return provider.loadSchematic(require(request)); }
    public MinecraftWorldOpsResult saveSchematic(MinecraftSchematicRequest request) { return provider.saveSchematic(require(request)); }
    public MinecraftWorldOpsResult undo(MinecraftWorldRef world) { return provider.undo(require(world)); }
    public MinecraftWorldOpsResult redo(MinecraftWorldRef world) { return provider.redo(require(world)); }

    private static <T> T require(T value) {
        return Objects.requireNonNull(value, "WorldOps request value");
    }
}
package org.tavall.ai.minecraft.worldops;

final class RecordingMinecraftWorldOpsProvider implements MinecraftWorldOpsProvider {
    private String operation;
    private Object request;

    String operation() { return operation; }
    Object request() { return request; }

    private MinecraftWorldOpsResult recorded(String operation, Object request) {
        this.operation = operation;
        this.request = request;
        return MinecraftWorldOpsResult.succeeded(operation);
    }

    @Override public MinecraftWorldOpsResult setBlock(MinecraftBlockSetRequest request) { return recorded("setBlock", request); }
    @Override public MinecraftWorldOpsResult setRegion(MinecraftRegionSetRequest request) { return recorded("setRegion", request); }
    @Override public MinecraftWorldOpsResult walls(MinecraftRegionSetRequest request) { return recorded("walls", request); }
    @Override public MinecraftWorldOpsResult replace(MinecraftRegionReplaceRequest request) { return recorded("replace", request); }
    @Override public MinecraftWorldOpsResult clear(MinecraftSelectionRequest request) { return recorded("clear", request); }
    @Override public MinecraftWorldOpsResult copy(MinecraftSelectionRequest request) { return recorded("copy", request); }
    @Override public MinecraftWorldOpsResult cut(MinecraftSelectionRequest request) { return recorded("cut", request); }
    @Override public MinecraftWorldOpsResult paste(MinecraftClipboardPasteRequest request) { return recorded("paste", request); }
    @Override public MinecraftWorldOpsResult rotate(MinecraftWorldRef world, MinecraftClipboardRotation rotation) { return recorded("rotate", rotation); }
    @Override public MinecraftWorldOpsResult flip(MinecraftWorldRef world, MinecraftFlipDirection direction) { return recorded("flip", direction); }
    @Override public MinecraftWorldOpsResult loadSchematic(MinecraftSchematicRequest request) { return recorded("loadSchematic", request); }
    @Override public MinecraftWorldOpsResult saveSchematic(MinecraftSchematicRequest request) { return recorded("saveSchematic", request); }
    @Override public MinecraftWorldOpsResult undo(MinecraftWorldRef world) { return recorded("undo", world); }
    @Override public MinecraftWorldOpsResult redo(MinecraftWorldRef world) { return recorded("redo", world); }
}
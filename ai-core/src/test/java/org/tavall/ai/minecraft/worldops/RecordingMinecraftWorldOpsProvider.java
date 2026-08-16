package org.tavall.ai.minecraft.worldops;

final class RecordingMinecraftWorldOpsProvider implements MinecraftWorldOpsProvider {
    private String operation;
    private Object request;
    private int operationSequence;

    String operation() { return operation; }
    Object request() { return request; }

    private MinecraftWorldOpsResult recorded(
            String operation,
            MinecraftWorldOpsOperationKind operationKind,
            Object request
    ) {
        this.operation = operation;
        this.request = request;
        operationSequence += 1;
        return MinecraftWorldOpsResult.succeeded(
                new MinecraftWorldOpsOperationId(operation + ":" + operationSequence),
                operationKind,
                operation);
    }

    @Override public MinecraftWorldOpsResult setBlock(MinecraftBlockSetRequest request) { return recorded("setBlock", MinecraftWorldOpsOperationKind.SET_BLOCK, request); }
    @Override public MinecraftWorldOpsResult setRegion(MinecraftRegionSetRequest request) { return recorded("setRegion", MinecraftWorldOpsOperationKind.SET_REGION, request); }
    @Override public MinecraftWorldOpsResult walls(MinecraftRegionSetRequest request) { return recorded("walls", MinecraftWorldOpsOperationKind.WALLS, request); }
    @Override public MinecraftWorldOpsResult replace(MinecraftRegionReplaceRequest request) { return recorded("replace", MinecraftWorldOpsOperationKind.REPLACE, request); }
    @Override public MinecraftWorldOpsResult clear(MinecraftSelectionRequest request) { return recorded("clear", MinecraftWorldOpsOperationKind.CLEAR, request); }
    @Override public MinecraftWorldOpsResult copy(MinecraftSelectionRequest request) { return recorded("copy", MinecraftWorldOpsOperationKind.COPY, request); }
    @Override public MinecraftWorldOpsResult cut(MinecraftSelectionRequest request) { return recorded("cut", MinecraftWorldOpsOperationKind.CUT, request); }
    @Override public MinecraftWorldOpsResult paste(MinecraftClipboardPasteRequest request) { return recorded("paste", MinecraftWorldOpsOperationKind.PASTE, request); }
    @Override public MinecraftWorldOpsResult rotate(MinecraftWorldRef world, MinecraftClipboardRotation rotation) { return recorded("rotate", MinecraftWorldOpsOperationKind.ROTATE, rotation); }
    @Override public MinecraftWorldOpsResult flip(MinecraftWorldRef world, MinecraftFlipDirection direction) { return recorded("flip", MinecraftWorldOpsOperationKind.FLIP, direction); }
    @Override public MinecraftWorldOpsResult loadSchematic(MinecraftSchematicRequest request) { return recorded("loadSchematic", MinecraftWorldOpsOperationKind.LOAD_SCHEMATIC, request); }
    @Override public MinecraftWorldOpsResult saveSchematic(MinecraftSchematicRequest request) { return recorded("saveSchematic", MinecraftWorldOpsOperationKind.SAVE_SCHEMATIC, request); }
    @Override public MinecraftWorldOpsResult undo(MinecraftWorldRef world) { return recorded("undo", MinecraftWorldOpsOperationKind.UNDO, world); }
    @Override public MinecraftWorldOpsResult redo(MinecraftWorldRef world) { return recorded("redo", MinecraftWorldOpsOperationKind.REDO, world); }
}

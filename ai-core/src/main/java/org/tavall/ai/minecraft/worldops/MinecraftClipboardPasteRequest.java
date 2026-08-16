package org.tavall.ai.minecraft.worldops;

import java.util.Objects;

public record MinecraftClipboardPasteRequest(
        MinecraftWorldRef world,
        MinecraftBlockPosition position,
        boolean atOriginalPosition
) {
    public MinecraftClipboardPasteRequest {
        Objects.requireNonNull(world, "world");
        if (!atOriginalPosition) {
            Objects.requireNonNull(position, "position");
        }
    }
}
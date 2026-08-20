package org.tavall.ai.minecraft.worldops;

import org.tavall.ai.core.annotation.AISchemaProperty;

import java.util.Objects;

public record MinecraftClipboardPasteRequest(
        MinecraftWorldRef world,
        @AISchemaProperty(required = false) MinecraftBlockPosition position,
        boolean atOriginalPosition
) {
    public MinecraftClipboardPasteRequest {
        Objects.requireNonNull(world, "world");
        if (!atOriginalPosition) {
            Objects.requireNonNull(position, "position");
        }
    }
}

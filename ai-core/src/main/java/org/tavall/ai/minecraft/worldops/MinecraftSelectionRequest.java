package org.tavall.ai.minecraft.worldops;

import java.util.Objects;

public record MinecraftSelectionRequest(MinecraftWorldRef world, MinecraftBlockRegion region) {
    public MinecraftSelectionRequest {
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(region, "region");
    }
}
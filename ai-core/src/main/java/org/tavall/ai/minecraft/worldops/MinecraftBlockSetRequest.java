package org.tavall.ai.minecraft.worldops;

import java.util.Objects;

public record MinecraftBlockSetRequest(
        MinecraftWorldRef world,
        MinecraftBlockPosition position,
        MinecraftBlockState block
) {
    public MinecraftBlockSetRequest {
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(position, "position");
        Objects.requireNonNull(block, "block");
    }
}
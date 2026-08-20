package org.tavall.ai.minecraft.worldops;

import java.util.Objects;

public record MinecraftRegionSetRequest(
        MinecraftWorldRef world,
        MinecraftBlockRegion region,
        MinecraftBlockState block
) {
    public MinecraftRegionSetRequest {
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(region, "region");
        Objects.requireNonNull(block, "block");
    }
}
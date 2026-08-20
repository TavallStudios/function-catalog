package org.tavall.ai.minecraft.worldops;

import java.util.Objects;

public record MinecraftRegionReplaceRequest(
        MinecraftWorldRef world,
        MinecraftBlockRegion region,
        MinecraftBlockState from,
        MinecraftBlockState to
) {
    public MinecraftRegionReplaceRequest {
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(region, "region");
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
    }
}
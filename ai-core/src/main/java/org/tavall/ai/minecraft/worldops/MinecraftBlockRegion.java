package org.tavall.ai.minecraft.worldops;

import java.util.Objects;

public record MinecraftBlockRegion(MinecraftBlockPosition min, MinecraftBlockPosition max) {
    public MinecraftBlockRegion {
        Objects.requireNonNull(min, "min");
        Objects.requireNonNull(max, "max");
        MinecraftBlockPosition normalizedMin = new MinecraftBlockPosition(
                Math.min(min.x(), max.x()), Math.min(min.y(), max.y()), Math.min(min.z(), max.z()));
        MinecraftBlockPosition normalizedMax = new MinecraftBlockPosition(
                Math.max(min.x(), max.x()), Math.max(min.y(), max.y()), Math.max(min.z(), max.z()));
        min = normalizedMin;
        max = normalizedMax;
    }
}
package org.tavall.ai.minecraft.worldops;

import java.util.Objects;

public record MinecraftWorldOpsResult(boolean success, String detail) {
    public MinecraftWorldOpsResult {
        detail = Objects.requireNonNullElse(detail, "");
    }

    public static MinecraftWorldOpsResult succeeded(String detail) {
        return new MinecraftWorldOpsResult(true, detail);
    }
}
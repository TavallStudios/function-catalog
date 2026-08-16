package org.tavall.ai.minecraft.worldops;

import java.util.Objects;

public record MinecraftWorldOpsOperationId(String value) {
    public MinecraftWorldOpsOperationId {
        value = Objects.requireNonNull(value, "value").trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("WorldOps operation id must not be blank");
        }
    }
}

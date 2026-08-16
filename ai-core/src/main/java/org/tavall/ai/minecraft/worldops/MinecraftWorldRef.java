package org.tavall.ai.minecraft.worldops;

import java.util.Objects;

public record MinecraftWorldRef(String value) {
    public MinecraftWorldRef {
        value = requireToken(value, "world");
    }

    static String requireToken(String value, String label) {
        Objects.requireNonNull(value, label);
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
        return normalized;
    }
}
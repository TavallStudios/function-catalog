package org.tavall.ai.minecraft.worldops;

import java.util.Objects;
import java.util.regex.Pattern;

public record MinecraftWorldRef(String value) {
    private static final Pattern WORLD_ID = Pattern.compile("[A-Za-z0-9_.:/-]{1,128}");

    public MinecraftWorldRef {
        Objects.requireNonNull(value, "world");
        value = value.trim();
        if (!WORLD_ID.matcher(value).matches() || value.contains("..")) {
            throw new IllegalArgumentException("invalid Minecraft world identity: " + value);
        }
    }
}
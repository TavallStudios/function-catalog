package org.tavall.ai.minecraft.worldops;

import java.util.Objects;
import java.util.regex.Pattern;

public record MinecraftBlockState(String value) {
    private static final Pattern BLOCK_STATE = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+(?:\\[[a-z0-9_=,.-]+])?");

    public MinecraftBlockState {
        Objects.requireNonNull(value, "value");
        value = value.trim();
        if (!BLOCK_STATE.matcher(value).matches()) {
            throw new IllegalArgumentException("invalid Minecraft block state: " + value);
        }
    }
}
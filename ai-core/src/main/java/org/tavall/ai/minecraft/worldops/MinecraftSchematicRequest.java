package org.tavall.ai.minecraft.worldops;

import java.util.Objects;
import java.util.regex.Pattern;

public record MinecraftSchematicRequest(MinecraftWorldRef world, String name, String format) {
    private static final Pattern NAME = Pattern.compile("[A-Za-z0-9_.-]{1,128}");

    public MinecraftSchematicRequest {
        Objects.requireNonNull(world, "world");
        name = Objects.requireNonNull(name, "name").trim();
        if (!NAME.matcher(name).matches()) {
            throw new IllegalArgumentException("invalid schematic name: " + name);
        }
        format = format == null ? "" : format.trim();
        if (!format.isEmpty() && !NAME.matcher(format).matches()) {
            throw new IllegalArgumentException("invalid schematic format: " + format);
        }
    }
}
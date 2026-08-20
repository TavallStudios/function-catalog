package org.tavall.ai.minecraft.worldops;

import org.tavall.ai.core.catalog.AIFunctionRegistrar;

import java.util.List;
import java.util.Objects;

public final class MinecraftWorldOpsRegistrar implements AIFunctionRegistrar {
    private final MinecraftWorldOpsProvider provider;

    public MinecraftWorldOpsRegistrar(MinecraftWorldOpsProvider provider) {
        this.provider = Objects.requireNonNull(provider, "provider");
    }

    @Override
    public Iterable<?> instances() {
        return List.of(new MinecraftWorldOpsFunctions(new MinecraftWorldOpsService(provider)));
    }
}
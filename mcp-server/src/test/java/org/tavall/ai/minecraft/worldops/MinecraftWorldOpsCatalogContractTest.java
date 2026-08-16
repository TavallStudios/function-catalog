package org.tavall.ai.minecraft.worldops;

import org.junit.jupiter.api.Test;
import org.tavall.ai.core.annotation.AIFunction;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

class MinecraftWorldOpsCatalogContractTest {
    private static final Set<String> EXPECTED_FUNCTIONS = Set.of(
            "minecraft_world_block_set",
            "minecraft_world_region_set",
            "minecraft_world_region_walls",
            "minecraft_world_region_replace",
            "minecraft_world_region_clear",
            "minecraft_world_clipboard_copy",
            "minecraft_world_clipboard_cut",
            "minecraft_world_clipboard_paste",
            "minecraft_world_clipboard_rotate",
            "minecraft_world_clipboard_flip",
            "minecraft_world_schematic_load",
            "minecraft_world_schematic_save",
            "minecraft_world_history_undo",
            "minecraft_world_history_redo"
    );

    @Test
    void publishesTheTypedInitialWorldOpsSurfaceWithoutGenericCommandEscapeHatches() {
        Class<?> functionsType;
        try {
            functionsType = Class.forName("org.tavall.ai.minecraft.worldops.MinecraftWorldOpsFunctions");
        } catch (ClassNotFoundException missingWorldOpsDomain) {
            fail("RED: MinecraftWorldOpsFunctions is not implemented yet", missingWorldOpsDomain);
            return;
        }

        Set<String> functionNames = Arrays.stream(functionsType.getDeclaredMethods())
                .map(method -> method.getAnnotation(AIFunction.class))
                .filter(annotation -> annotation != null)
                .map(AIFunction::name)
                .collect(Collectors.toSet());

        assertEquals(EXPECTED_FUNCTIONS, functionNames);
        assertFalse(functionNames.stream().anyMatch(this::isGenericEscapeHatch));
    }

    private boolean isGenericEscapeHatch(String functionName) {
        String normalized = functionName.toLowerCase();
        return normalized.equals("minecraft_world_command")
                || normalized.contains("worldedit_command")
                || normalized.contains("chat_command")
                || normalized.contains("shell");
    }
}

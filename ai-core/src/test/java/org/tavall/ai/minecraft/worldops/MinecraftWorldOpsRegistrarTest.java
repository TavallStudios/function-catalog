package org.tavall.ai.minecraft.worldops;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.tavall.ai.core.catalog.AIFunctionCatalog;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MinecraftWorldOpsRegistrarTest {
    private static final Set<String> FUNCTION_NAMES = Set.of(
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

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void registersCanonicalFunctionsAndDelegatesTypedBlockMutation() {
        RecordingMinecraftWorldOpsProvider provider = new RecordingMinecraftWorldOpsProvider();
        AIFunctionCatalog catalog = new AIFunctionCatalog(objectMapper);
        catalog.registerRegistrars(Set.of(new MinecraftWorldOpsRegistrar(provider)));

        assertEquals(FUNCTION_NAMES, catalog.getFunctionDefinitions().keySet());

        ObjectNode arguments = objectMapper.createObjectNode();
        ObjectNode request = arguments.putObject("request");
        request.putObject("world").put("value", "kingdom-east");
        ObjectNode position = request.putObject("position");
        position.put("x", 12).put("y", 80).put("z", -4);
        request.putObject("block").put("value", "minecraft:stone_bricks");

        Object result = catalog.invoke("minecraft_world_block_set", arguments);

        MinecraftWorldOpsResult typedResult = assertInstanceOf(MinecraftWorldOpsResult.class, result);
        assertTrue(typedResult.success());
        assertEquals(new MinecraftWorldOpsOperationId("setBlock:1"), typedResult.operationId());
        assertEquals(MinecraftWorldOpsOperationKind.SET_BLOCK, typedResult.operationKind());
        assertEquals("setBlock", provider.operation());
        MinecraftBlockSetRequest typedRequest = assertInstanceOf(MinecraftBlockSetRequest.class, provider.request());
        assertEquals(new MinecraftWorldRef("kingdom-east"), typedRequest.world());
        assertEquals(new MinecraftBlockPosition(12, 80, -4), typedRequest.position());
        assertEquals(new MinecraftBlockState("minecraft:stone_bricks"), typedRequest.block());
    }

    @Test
    void publishesOriginalPositionPasteWithoutRequiringSyntheticCoordinates() {
        RecordingMinecraftWorldOpsProvider provider = new RecordingMinecraftWorldOpsProvider();
        AIFunctionCatalog catalog = new AIFunctionCatalog(objectMapper);
        catalog.registerRegistrars(Set.of(new MinecraftWorldOpsRegistrar(provider)));

        JsonNode requestSchema = catalog.getFunctionDefinitions()
                .get("minecraft_world_clipboard_paste")
                .getCanonicalParametersSchema()
                .path("properties")
                .path("request");

        assertTrue(requestSchema.path("required").toString().contains("world"));
        assertTrue(requestSchema.path("required").toString().contains("atOriginalPosition"));
        assertFalse(requestSchema.path("required").toString().contains("position"));
        assertEquals("object", requestSchema.path("properties").path("position").path("type").asText());

        ObjectNode arguments = objectMapper.createObjectNode();
        ObjectNode request = arguments.putObject("request");
        request.putObject("world").put("value", "kingdom-east");
        request.put("atOriginalPosition", true);

        MinecraftWorldOpsResult result = assertInstanceOf(
                MinecraftWorldOpsResult.class,
                catalog.invoke("minecraft_world_clipboard_paste", arguments)
        );
        assertTrue(result.success());
        MinecraftClipboardPasteRequest typedRequest = assertInstanceOf(
                MinecraftClipboardPasteRequest.class,
                provider.request()
        );
        assertTrue(typedRequest.atOriginalPosition());
        assertEquals(null, typedRequest.position());
    }

    @Test
    void normalizesRegionsAndRejectsCommandShapedIdentifiers() {
        MinecraftBlockRegion region = new MinecraftBlockRegion(
                new MinecraftBlockPosition(9, 70, 12),
                new MinecraftBlockPosition(-2, 64, 3));

        assertEquals(new MinecraftBlockPosition(-2, 64, 3), region.min());
        assertEquals(new MinecraftBlockPosition(9, 70, 12), region.max());
        assertThrows(IllegalArgumentException.class, () -> new MinecraftWorldRef("../world; //set lava"));
        assertThrows(IllegalArgumentException.class, () -> new MinecraftBlockState("minecraft:stone; //set lava"));
        assertThrows(IllegalArgumentException.class, () -> new MinecraftWorldOpsOperationId("  "));
        assertThrows(
                IllegalArgumentException.class,
                () -> new MinecraftSchematicRequest(new MinecraftWorldRef("kingdom-east"), "../../escape", "sponge.3"));
    }
}

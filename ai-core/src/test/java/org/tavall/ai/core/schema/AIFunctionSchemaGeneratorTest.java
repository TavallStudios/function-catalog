package org.tavall.ai.core.schema;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AIFunctionSchemaGeneratorTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AIFunctionSchemaGenerator schemaGenerator = new AIFunctionSchemaGenerator(objectMapper);

    @Test
    void generatesSchemaForPojoContainingEnumListAndRecord() {
        JavaType javaType = objectMapper.getTypeFactory().constructType(SchemaPojoPayload.class);
        ObjectNode schema = schemaGenerator.generateTypeSchema(javaType);

        assertEquals("object", schema.path("type").asText());
        assertEquals("string", schema.path("properties").path("status").path("type").asText());
        assertEquals(2, schema.path("properties").path("status").path("enum").size());

        ObjectNode itemsSchema = (ObjectNode) schema.path("properties").path("items");
        assertEquals("array", itemsSchema.path("type").asText());
        assertEquals("object", itemsSchema.path("items").path("type").asText());
        assertEquals("integer", itemsSchema.path("items").path("properties").path("count").path("type").asText());
        assertEquals("string", itemsSchema.path("items").path("properties").path("label").path("type").asText());
    }

    @Test
    void generatesMapAndSetSchemas() {
        JavaType javaType = objectMapper.getTypeFactory().constructType(SchemaCollectionPayload.class);
        ObjectNode schema = schemaGenerator.generateTypeSchema(javaType);

        assertEquals("object", schema.path("type").asText());
        assertEquals(
                "object",
                schema.path("properties").path("payloadByKey").path("additionalProperties").path("type").asText()
        );
        assertEquals("array", schema.path("properties").path("ids").path("type").asText());
        assertEquals("integer", schema.path("properties").path("ids").path("items").path("type").asText());
        assertTrue(schema.path("properties").path("payloadByKey").path("additionalProperties").has("properties"));
    }
}
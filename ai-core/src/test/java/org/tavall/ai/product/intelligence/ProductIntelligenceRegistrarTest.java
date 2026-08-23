package org.tavall.ai.product.intelligence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.tavall.ai.core.catalog.AIFunctionCatalog;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductIntelligenceRegistrarTest {
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void registersCanonicalReadAndAtomicRecordFunctions() {
        RecordingProvider provider = new RecordingProvider();
        AIFunctionCatalog catalog = new AIFunctionCatalog(mapper);
        catalog.registerRegistrars(Set.of(new ProductIntelligenceRegistrar(provider)));

        assertEquals(Set.of("product_intelligence_read", "product_intelligence_record"),
                catalog.getFunctionDefinitions().keySet());

        ObjectNode recordArgs = mapper.createObjectNode();
        var entries = recordArgs.putObject("request").putArray("entries");
        entries.add(entryJson("winner", "ACCEPTED"));
        entries.add(entryJson("alternate", "REJECTED"));

        ProductIntelligenceRecordResult recorded = assertInstanceOf(
                ProductIntelligenceRecordResult.class,
                catalog.invoke("product_intelligence_record", recordArgs));
        assertEquals(2, recorded.recordedCount());
        assertEquals(1, provider.batches.size());
        assertEquals(2, provider.batches.getFirst().size());

        ObjectNode readArgs = mapper.createObjectNode();
        readArgs.putObject("request").put("productId", "novus-web").put("agentId", "web");
        ProductIntelligenceReadResult read = assertInstanceOf(
                ProductIntelligenceReadResult.class,
                catalog.invoke("product_intelligence_read", readArgs));
        assertEquals(2, read.entries().size());
    }

    @Test
    void recordBatchRejectsMixedScopeAndSchemasExposeTypedRequest() {
        assertThrows(IllegalArgumentException.class, () -> new ProductIntelligenceRecordRequest(List.of(
                entry("a", "novus-web", "web"),
                entry("b", "tavall-pvp", "web")
        )));

        AIFunctionCatalog catalog = new AIFunctionCatalog(mapper);
        catalog.registerRegistrars(Set.of(new ProductIntelligenceRegistrar(new RecordingProvider())));
        var schema = catalog.getFunctionDefinitions().get("product_intelligence_record")
                .getCanonicalParametersSchema();
        assertEquals("object", schema.path("properties").path("request").path("type").asText());
        assertTrue(schema.toString().contains("entries"));
    }

    private ObjectNode entryJson(String entryId, String disposition) {
        ObjectNode node = mapper.createObjectNode();
        node.put("entryId", entryId);
        node.put("productId", "novus-web");
        node.put("agentId", "web");
        node.put("category", "design-decision");
        node.put("key", "home/" + entryId);
        node.put("value", entryId);
        node.put("rationale", "A/B evidence");
        node.put("disposition", disposition);
        node.putArray("evidenceReferences").add("artifact://" + entryId);
        node.put("recordedAt", "2026-08-23T10:00:00Z");
        return node;
    }

    private ProductIntelligenceEntry entry(String id, String productId, String agentId) {
        return new ProductIntelligenceEntry(
                id, productId, agentId, "design-decision", "home/" + id, id, "A/B evidence",
                ProductIntelligenceDisposition.REFERENCE, Set.of(), Instant.parse("2026-08-23T10:00:00Z"));
    }

    private static final class RecordingProvider implements ProductIntelligenceProvider {
        private final List<List<ProductIntelligenceEntry>> batches = new ArrayList<>();

        @Override
        public List<ProductIntelligenceEntry> read(String productId, String agentId) {
            return batches.stream().flatMap(List::stream)
                    .filter(entry -> productId.equals(entry.productId()) && agentId.equals(entry.agentId()))
                    .toList();
        }

        @Override
        public void recordBatch(List<ProductIntelligenceEntry> entries) {
            batches.add(List.copyOf(entries));
        }
    }
}

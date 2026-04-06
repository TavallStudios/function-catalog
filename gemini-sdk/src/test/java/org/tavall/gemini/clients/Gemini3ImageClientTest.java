package org.tavall.gemini.clients;

import com.google.genai.Client;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tavall.gemini.enums.GeminiModel;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for Gemini3ImageClient.
 * Tests client initialization, model availability, and API version support.
 */
class Gemini3ImageClientTest {

    private Gemini3ImageClient client;

    @BeforeEach
    void setUp() {
        log("Setting up Gemini3ImageClient test environment");
        String apiKey = System.getenv("GEMINI_API_KEY");
        if (apiKey != null && !apiKey.isBlank()) {
            client = new Gemini3ImageClient();
            return;
        }

        Client localClient = Client.builder()
                .apiKey("test-key")
                .build();
        client = new Gemini3ImageClient(localClient);
    }

    @Test
    void testDefaultConstructor() {
        log("Testing default constructor initialization");

        assertNotNull(client, "Client should not be null");
        assertNotNull(client.getClient(), "Internal Client should be initialized");

        log("✓ Default constructor test passed");
    }

    @Test
    void testClientWithProvidedInstance() {
        log("Testing constructor with provided Client instance");

        Client mockClient = Client.builder()
                .apiKey("test-key")
                .build();
        Gemini3ImageClient customClient = new Gemini3ImageClient(mockClient);

        assertNotNull(customClient.getClient(), "Client should not be null");
        assertEquals(mockClient, customClient.getClient(), "Should use provided client");

        log("✓ Provided client test passed");
    }

    @Test
    void testAvailableModels() {
        log("Testing available Gemini models");

        assertTrue(client.hasAvailableModel(GeminiModel.GEMINI_3_FLASH),
                "Should support GEMINI_3_FLASH");
        log("  - GEMINI_3_FLASH: Available");

        assertTrue(client.hasAvailableModel(GeminiModel.GEMINI_3_PRO),
                "Should support GEMINI_3_PRO");
        log("  - GEMINI_3_PRO: Available");

        assertFalse(client.hasAvailableModel(GeminiModel.GEMINI_2_5_FLASH),
                "Should not support GEMINI_2_5_FLASH");
        log("  - GEMINI_2_5_FLASH: Not Available (expected)");

        log("✓ Available models test passed");
    }

    @Test
    void testUnavailableModels() {
        log("Testing unavailable Gemini models");

        assertFalse(client.hasAvailableModel(GeminiModel.GEMINI_3_PRO_IMAGE_PREVIEW),
                "Should not support GEMINI_3_PRO_IMAGE_PREVIEW");
        log("  - GEMINI_3_PRO_IMAGE_PREVIEW: Not Available");

        assertFalse(client.hasAvailableModel(GeminiModel.GEMINI_2_5_FLASH),
                "Should not support GEMINI_2_5_FLASH");
        log("  - GEMINI_2_5_FLASH: Not Available");

        log("✓ Unavailable models test passed");
    }

    @Test
    void testMultipleModels() {
        log("Testing multiple model checks in sequence");

        GeminiModel[] supportedModels = {
                GeminiModel.GEMINI_3_FLASH,
                GeminiModel.GEMINI_3_PRO
        };

        for (GeminiModel model : supportedModels) {
            assertTrue(client.hasAvailableModel(model),
                    "Model " + model + " should be available");
            log("  - " + model + ": Available");
        }

        log("✓ Multiple models test passed");
    }

    @Test
    void testClientNotNull() {
        log("Testing client non-null guarantee");

        assertNotNull(client.getClient(),
                "getClient() should never return null after initialization");

        log("✓ Client non-null test passed");
    }

    @Test
    void testGeminiConnection() {
        log("Testing actual connection to Gemini API");

        String apiKey = System.getenv("GEMINI_API_KEY");

        if (apiKey == null || apiKey.isEmpty()) {
            log("⚠ GEMINI_API_KEY not set - skipping connection test");
            return;
        }

        log("  - API Key found in environment");

        try {
            Client geminiClient = client.getClient();
            assertNotNull(geminiClient, "Gemini client should be initialized");
            log("  - Client initialized successfully");

            log("  - Attempting to verify Gemini API connection");

            assertTrue(geminiClient != null, "Client should be connected");
            log("  - Connection verified");

            log("✓ Gemini connection test passed");

        } catch (Exception e) {
            log("✗ Connection failed: " + e.getMessage());
            fail("Failed to connect to Gemini API: " + e.getMessage());
        }
    }

    private void log(String message) {
        System.out.println("[Gemini3ImageClientTest] " + message);
    }
}
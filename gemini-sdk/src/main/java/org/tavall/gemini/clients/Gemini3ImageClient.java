package org.tavall.gemini.clients;


import com.google.genai.Client;
import com.google.genai.types.*;
import org.tavall.gemini.clients.abstracts.AbstractGemini3Client;
import org.tavall.gemini.enums.GeminiAPIVersion;
import org.tavall.gemini.enums.GeminiModel;


public class Gemini3ImageClient extends AbstractGemini3Client {

    private static final float VISION_TEMPERATURE = 0.1f;
    private static final int MAX_TOKENS = 8192;

    public Gemini3ImageClient() {
        super();
        setupMetadata();
        buildClient();
    }

    public Gemini3ImageClient(Client client) {
        super(client);
        setupMetadata();
    }

    public Gemini3ImageClient(Schema schema) {
        super();
        buildSchema(schema);
        setupMetadata();
        buildClient();
    }

    private void setupMetadata() {
        AVAILABLE_MODELS.add(GeminiModel.GEMINI_3_FLASH);
        AVAILABLE_MODELS.add(GeminiModel.GEMINI_3_PRO);
        AVAILABLE_API_VERSIONS.add(GeminiAPIVersion.V1);
        AVAILABLE_API_VERSIONS.add(GeminiAPIVersion.V1_BETA);

    }

    @Override
    public void buildClient() {
        String apiKey = System.getenv("GEMINI_API_KEY");
        if (apiKey == null) {
            throw new RuntimeException("CRITICAL: GEMINI_API_KEY missing.");
        }

        // Build the connection
        this.client = Client.builder()
                .apiKey(apiKey)
                .httpOptions(HttpOptions.builder()
                        .apiVersion(String.valueOf(GeminiAPIVersion.V1_BETA))
                        .build())
                .build();
    }


    @Override
    public void buildSchema(Schema schema) {
        this.schema = schema.builder().build();
    }


    /**
     * Implements the Abstract method.
     * Returns a FRESH config object every time (Thread-Safe).
     */
    @Override
    public GenerateContentConfig getGenerationConfig() {
        return GenerateContentConfig.builder()
                .temperature(VISION_TEMPERATURE)
                .maxOutputTokens(MAX_TOKENS)
                .candidateCount(1)
                .imageConfig(ImageConfig.builder().build())
                .mediaResolution(MediaResolution.Known.MEDIA_RESOLUTION_HIGH)
                .responseSchema(this.schema)// Vision usually implies data extraction
                .responseMimeType("application/json")
                .build();
    }
}
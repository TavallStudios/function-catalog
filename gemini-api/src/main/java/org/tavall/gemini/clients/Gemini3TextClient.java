package org.tavall.gemini.clients;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.HttpOptions;
import com.google.genai.types.Schema;
import org.tavall.gemini.clients.abstracts.AbstractGemini3Client;
import org.tavall.gemini.enums.GeminiAPIVersion;
import org.tavall.gemini.enums.GeminiModel;

public class Gemini3TextClient extends AbstractGemini3Client {

    private static final float TEXT_TEMPERATURE = 0.2f;
    private static final int MAX_TOKENS = 4096;

    public Gemini3TextClient() {
        super();
        setupMetadata();
        buildClient();
    }

    public Gemini3TextClient(Client client) {
        super(client);
        setupMetadata();
    }

    public Gemini3TextClient(Schema schema) {
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

    @Override
    public GenerateContentConfig getGenerationConfig() {
        GenerateContentConfig.Builder builder = GenerateContentConfig.builder()
                .temperature(TEXT_TEMPERATURE)
                .maxOutputTokens(MAX_TOKENS)
                .candidateCount(1);

        if (this.schema != null) {
            builder.responseSchema(this.schema)
                    .responseMimeType("application/json");
        }

        return builder.build();
    }
}

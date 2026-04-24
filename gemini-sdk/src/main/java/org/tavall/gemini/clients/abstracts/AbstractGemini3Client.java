package org.tavall.gemini.clients.abstracts;


import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.Schema;
import org.tavall.gemini.enums.GeminiAPIVersion;
import org.tavall.gemini.enums.GeminiModel;

import java.util.ArrayList;
import java.util.List;
//TODO: Add javadoc for client and client config creation
public abstract class AbstractGemini3Client {

    protected Client client;
    protected Schema schema;
    protected List<GeminiModel> AVAILABLE_MODELS = new ArrayList<>();
    protected List<GeminiAPIVersion> AVAILABLE_API_VERSIONS = new ArrayList<>();

    public AbstractGemini3Client() {
    }

    public AbstractGemini3Client(Client client) {
        this.client = client;
    }

    public Client getClient() {
        if (this.client == null) {
            // TODO: Make custom exception here
            throw new IllegalStateException("Gemini Client is not initialized. Call buildClient() first.");
        }
        return this.client;
    }
    public Schema getSchema() {
        if (this.schema == null) {
            // TODO: Make custom exception here
            throw new IllegalStateException("Gemini Client schema is not initialized. ");
        }
        return this.schema;
    }



    public abstract GenerateContentConfig getGenerationConfig();

    public abstract void buildClient();
    public abstract void buildSchema(Schema schema);

    // --- Validation Helpers ---
    public boolean hasAvailableModel(GeminiModel geminiModel) {
        return AVAILABLE_MODELS.contains(geminiModel);
    }
}
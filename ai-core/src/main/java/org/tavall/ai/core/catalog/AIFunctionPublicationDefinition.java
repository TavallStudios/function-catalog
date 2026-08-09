package org.tavall.ai.core.catalog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.Objects;

/**
 * Immutable publication metadata for one Function Catalog entry.
 *
 * <p>This type deliberately retains no {@link java.lang.reflect.Method}, target instance,
 * {@link Class}, or Jackson {@code JavaType}. It is safe to hand to capability-scoped agents for
 * discovery and filtering without also handing them the catalog's live invocation objects.</p>
 */
public final class AIFunctionPublicationDefinition {
    private final String name;
    private final String description;
    private final String signature;
    private final List<AIFunctionPublicationParameterDefinition> parameters;
    private final List<String> requiredParameters;
    private final ObjectNode canonicalParametersSchema;
    private final String ownerTypeName;
    private final AIFunctionRegistrationSource registrationSource;
    private final boolean enabled;
    private final ObjectNode settingsSchema;
    private final JsonNode settings;
    private final JsonNode defaultSettings;
    private final String settingsDescription;
    private final String updatedAt;

    private AIFunctionPublicationDefinition(
            String name,
            String description,
            String signature,
            List<AIFunctionPublicationParameterDefinition> parameters,
            List<String> requiredParameters,
            ObjectNode canonicalParametersSchema,
            String ownerTypeName,
            AIFunctionRegistrationSource registrationSource,
            boolean enabled,
            ObjectNode settingsSchema,
            JsonNode settings,
            JsonNode defaultSettings,
            String settingsDescription,
            String updatedAt
    ) {
        this.name = requireText(name, "name");
        this.description = description == null ? "" : description;
        this.signature = requireText(signature, "signature");
        this.parameters = List.copyOf(Objects.requireNonNull(parameters, "parameters"));
        this.requiredParameters = List.copyOf(Objects.requireNonNull(requiredParameters, "requiredParameters"));
        this.canonicalParametersSchema = Objects.requireNonNull(
                canonicalParametersSchema,
                "canonicalParametersSchema"
        ).deepCopy();
        this.ownerTypeName = requireText(ownerTypeName, "ownerTypeName");
        this.registrationSource = Objects.requireNonNull(registrationSource, "registrationSource");
        this.enabled = enabled;
        this.settingsSchema = settingsSchema == null ? null : settingsSchema.deepCopy();
        this.settings = settings == null ? null : settings.deepCopy();
        this.defaultSettings = defaultSettings == null ? null : defaultSettings.deepCopy();
        this.settingsDescription = settingsDescription == null ? "" : settingsDescription;
        this.updatedAt = updatedAt == null ? "" : updatedAt;
    }

    static AIFunctionPublicationDefinition from(AIFunctionDefinition definition) {
        AIFunctionDefinition safeDefinition = Objects.requireNonNull(definition, "definition");
        List<AIFunctionPublicationParameterDefinition> publishedParameters = safeDefinition.getParameters().stream()
                .map(parameter -> new AIFunctionPublicationParameterDefinition(
                        parameter.getIndex(),
                        parameter.getName(),
                        parameter.getDescription(),
                        parameter.getJavaType().toCanonical(),
                        parameter.isRequired()
                ))
                .toList();
        return new AIFunctionPublicationDefinition(
                safeDefinition.getName(),
                safeDefinition.getDescription(),
                safeDefinition.getSignature(),
                publishedParameters,
                safeDefinition.getRequiredParameters(),
                safeDefinition.getCanonicalParametersSchema(),
                safeDefinition.getOwnerType().getName(),
                safeDefinition.getRegistrationSource(),
                safeDefinition.isEnabled(),
                safeDefinition.getSettingsSchema(),
                safeDefinition.getSettings(),
                safeDefinition.getDefaultSettings(),
                safeDefinition.getSettingsDescription(),
                safeDefinition.getUpdatedAt()
        );
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getSignature() {
        return signature;
    }

    public List<AIFunctionPublicationParameterDefinition> getParameters() {
        return parameters;
    }

    public List<String> getRequiredParameters() {
        return requiredParameters;
    }

    public ObjectNode getCanonicalParametersSchema() {
        return canonicalParametersSchema.deepCopy();
    }

    public String getOwnerTypeName() {
        return ownerTypeName;
    }

    public AIFunctionRegistrationSource getRegistrationSource() {
        return registrationSource;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean hasSettings() {
        return settingsSchema != null;
    }

    public ObjectNode getSettingsSchema() {
        return settingsSchema == null ? null : settingsSchema.deepCopy();
    }

    public JsonNode getSettings() {
        return settings == null ? null : settings.deepCopy();
    }

    public JsonNode getDefaultSettings() {
        return defaultSettings == null ? null : defaultSettings.deepCopy();
    }

    public String getSettingsDescription() {
        return settingsDescription;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    private static String requireText(String value, String fieldName) {
        if (value != null && !value.isBlank()) {
            return value;
        }
        throw new IllegalArgumentException(fieldName + " must not be blank");
    }
}

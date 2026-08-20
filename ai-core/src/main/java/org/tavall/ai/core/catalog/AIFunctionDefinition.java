package org.tavall.ai.core.catalog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.tavall.ai.core.invocation.AIFunctionOutput;
import org.tavall.ai.core.schema.AIFunctionSchemaGenerator;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class AIFunctionDefinition {
    private static final AIFunctionSchemaGenerator OUTPUT_SCHEMA_GENERATOR =
            new AIFunctionSchemaGenerator(new ObjectMapper());

    private final String name;
    private final String description;
    private final String signature;
    private final List<AIFunctionParameterDefinition> parameters;
    private final List<String> requiredParameters;
    private final ObjectNode canonicalParametersSchema;
    private final ObjectNode canonicalOutputSchema;
    private final Method method;
    private final Object target;
    private final Class<?> ownerType;
    private final AIFunctionRegistrationSource registrationSource;
    private final boolean enabled;
    private final ObjectNode settingsSchema;
    private final JsonNode settings;
    private final JsonNode defaultSettings;
    private final String settingsDescription;
    private final String updatedAt;

    public AIFunctionDefinition(
            String name,
            String description,
            String signature,
            List<AIFunctionParameterDefinition> parameters,
            List<String> requiredParameters,
            ObjectNode canonicalParametersSchema,
            Method method,
            Object target,
            Class<?> ownerType,
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
        this.parameters = Collections.unmodifiableList(new ArrayList<>(requireValue(parameters, "parameters")));
        this.requiredParameters = Collections.unmodifiableList(new ArrayList<>(requireValue(requiredParameters, "requiredParameters")));
        this.canonicalParametersSchema = requireValue(canonicalParametersSchema, "canonicalParametersSchema").deepCopy();
        this.method = requireValue(method, "method");
        this.canonicalOutputSchema = canonicalOutputSchema(method);
        this.target = target;
        this.ownerType = requireValue(ownerType, "ownerType");
        this.registrationSource = requireValue(registrationSource, "registrationSource");
        this.enabled = enabled;
        this.settingsSchema = settingsSchema == null ? null : settingsSchema.deepCopy();
        this.settings = settings == null ? null : settings.deepCopy();
        this.defaultSettings = defaultSettings == null ? null : defaultSettings.deepCopy();
        this.settingsDescription = settingsDescription == null ? "" : settingsDescription;
        this.updatedAt = updatedAt == null ? "" : updatedAt;
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

    public List<AIFunctionParameterDefinition> getParameters() {
        return parameters;
    }

    public List<String> getRequiredParameters() {
        return requiredParameters;
    }

    public ObjectNode getCanonicalParametersSchema() {
        return canonicalParametersSchema.deepCopy();
    }

    public ObjectNode getCanonicalOutputSchema() {
        return canonicalOutputSchema == null ? null : canonicalOutputSchema.deepCopy();
    }

    /** Trusted catalog/runtime API. Capability-scoped views never publish this object. */
    public Method getMethod() {
        return method;
    }

    /** Trusted catalog/runtime API. Capability-scoped views never publish this object. */
    public Object getTarget() {
        return target;
    }

    public Class<?> getOwnerType() {
        return ownerType;
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

    public boolean isStatic() {
        return java.lang.reflect.Modifier.isStatic(method.getModifiers());
    }

    private static ObjectNode canonicalOutputSchema(Method method) {
        Class<?> returnType = method.getReturnType();
        if (void.class.equals(returnType)
                || Void.class.equals(returnType)
                || AIFunctionOutput.class.isAssignableFrom(returnType)) {
            return null;
        }
        return OUTPUT_SCHEMA_GENERATOR.generateTypeSchema(method.getGenericReturnType());
    }

    private static <T> T requireValue(T value, String fieldName) {
        if (value != null) {
            return value;
        }

        throw new IllegalArgumentException(fieldName + " must not be null");
    }

    private static String requireText(String value, String fieldName) {
        if (value != null && !value.isBlank()) {
            return value;
        }

        throw new IllegalArgumentException(fieldName + " must not be blank");
    }
}

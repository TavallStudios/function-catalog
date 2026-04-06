package org.tavall.ai.core.schema;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.tavall.ai.core.catalog.AIFunctionParameterDefinition;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class AIFunctionSchemaGenerator {
    private final ObjectMapper objectMapper;

    public AIFunctionSchemaGenerator(ObjectMapper objectMapper) {
        this.objectMapper = requireValue(objectMapper, "objectMapper");
    }

    public ObjectNode generateInputSchema(List<AIFunctionParameterDefinition> parameters) {
        List<AIFunctionParameterDefinition> parameterList = new ArrayList<>(requireValue(parameters, "parameters"));
        parameterList.sort(Comparator.comparing(AIFunctionParameterDefinition::getName));

        ObjectNode rootSchema = objectMapper.createObjectNode();
        rootSchema.put("type", "object");

        ObjectNode properties = rootSchema.putObject("properties");
        ArrayNode required = rootSchema.putArray("required");

        for (AIFunctionParameterDefinition parameterDefinition : parameterList) {
            ObjectNode parameterSchema = createTypeSchema(parameterDefinition.getJavaType(), new LinkedHashSet<>());
            if (!parameterDefinition.getDescription().isBlank()) {
                parameterSchema.put("description", parameterDefinition.getDescription());
            }

            properties.set(parameterDefinition.getName(), parameterSchema);
            if (parameterDefinition.isRequired()) {
                required.add(parameterDefinition.getName());
            }
        }

        rootSchema.put("additionalProperties", false);
        return rootSchema;
    }

    public ObjectNode generateTypeSchema(JavaType type) {
        return createTypeSchema(requireValue(type, "type"), new LinkedHashSet<>());
    }

    public ObjectNode generateTypeSchema(Class<?> type) {
        return generateTypeSchema(objectMapper.getTypeFactory().constructType(requireValue(type, "type")));
    }

    private ObjectNode createTypeSchema(JavaType javaType, Set<String> activeTypes) {
        JavaType safeType = normalize(javaType);
        Class<?> rawClass = safeType.getRawClass();

        if (Optional.class.isAssignableFrom(rawClass)) {
            JavaType contentType = safeType.containedTypeCount() > 0 ? safeType.containedType(0) : objectMapper.getTypeFactory().constructType(Object.class);
            return createTypeSchema(contentType, activeTypes);
        }

        if (isBooleanType(rawClass)) {
            return typeOnlySchema("boolean");
        }

        if (isIntegerType(rawClass)) {
            return typeOnlySchema("integer");
        }

        if (isNumberType(rawClass)) {
            return typeOnlySchema("number");
        }

        if (isStringType(rawClass)) {
            return typeOnlySchema("string");
        }

        if (rawClass.isEnum()) {
            return enumSchema(rawClass);
        }

        if (com.fasterxml.jackson.databind.JsonNode.class.isAssignableFrom(rawClass)) {
            return typeOnlySchema("object");
        }

        String canonicalTypeName = safeType.toCanonical();
        if (activeTypes.contains(canonicalTypeName)) {
            return typeOnlySchema("object");
        }

        activeTypes.add(canonicalTypeName);
        try {
            if (rawClass.isArray()) {
                return arraySchema(objectMapper.getTypeFactory().constructType(rawClass.getComponentType()), activeTypes);
            }

            if (Collection.class.isAssignableFrom(rawClass)) {
                return arraySchema(safeType.getContentType(), activeTypes);
            }

            if (Map.class.isAssignableFrom(rawClass)) {
                return mapSchema(safeType, activeTypes);
            }

            if (rawClass.isRecord()) {
                return recordSchema(safeType, activeTypes);
            }

            if (Object.class.equals(rawClass)) {
                return typeOnlySchema("object");
            }

            return pojoSchema(safeType, activeTypes);
        } finally {
            activeTypes.remove(canonicalTypeName);
        }
    }

    private ObjectNode arraySchema(JavaType contentType, Set<String> activeTypes) {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "array");
        JavaType safeContentType = contentType == null
                ? objectMapper.getTypeFactory().constructType(Object.class)
                : contentType;
        schema.set("items", createTypeSchema(safeContentType, activeTypes));
        return schema;
    }

    private ObjectNode mapSchema(JavaType mapType, Set<String> activeTypes) {
        JavaType keyType = mapType.getKeyType();
        if (keyType != null && !isStringMapKey(keyType.getRawClass())) {
            throw new IllegalArgumentException("Map key type must be String-compatible: " + keyType.toCanonical());
        }

        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        JavaType valueType = mapType.getContentType();
        if (valueType == null) {
            schema.set("additionalProperties", typeOnlySchema("object"));
            return schema;
        }

        schema.set("additionalProperties", createTypeSchema(valueType, activeTypes));
        return schema;
    }

    private ObjectNode recordSchema(JavaType recordType, Set<String> activeTypes) {
        Class<?> rawClass = recordType.getRawClass();
        List<RecordComponent> components = new ArrayList<>(Arrays.asList(rawClass.getRecordComponents()));
        components.sort(Comparator.comparing(RecordComponent::getName));

        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        ArrayNode required = schema.putArray("required");

        for (RecordComponent component : components) {
            JavaType componentType = objectMapper.getTypeFactory().constructType(component.getGenericType());
            properties.set(component.getName(), createTypeSchema(componentType, activeTypes));
            required.add(component.getName());
        }

        schema.put("additionalProperties", false);
        return schema;
    }

    private ObjectNode pojoSchema(JavaType pojoType, Set<String> activeTypes) {
        try {
            BeanInfo beanInfo = Introspector.getBeanInfo(pojoType.getRawClass(), Object.class);
            List<PropertyDescriptor> descriptors = new ArrayList<>();
            for (PropertyDescriptor descriptor : beanInfo.getPropertyDescriptors()) {
                if (descriptor.getReadMethod() != null && descriptor.getReadMethod().getParameterCount() == 0) {
                    descriptors.add(descriptor);
                }
            }

            descriptors.sort(Comparator.comparing(PropertyDescriptor::getName));

            ObjectNode schema = objectMapper.createObjectNode();
            schema.put("type", "object");
            ObjectNode properties = schema.putObject("properties");
            ArrayNode required = schema.putArray("required");

            for (PropertyDescriptor descriptor : descriptors) {
                JavaType propertyType = objectMapper.getTypeFactory().constructType(descriptor.getReadMethod().getGenericReturnType());
                properties.set(descriptor.getName(), createTypeSchema(propertyType, activeTypes));
                if (descriptor.getReadMethod().getReturnType().isPrimitive()) {
                    required.add(descriptor.getName());
                }
            }

            schema.put("additionalProperties", false);
            return schema;
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to introspect POJO type: " + pojoType.toCanonical(), exception);
        }
    }

    private ObjectNode enumSchema(Class<?> enumClass) {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "string");
        ArrayNode enumValues = schema.putArray("enum");
        List<String> names = new ArrayList<>();
        for (Object constant : enumClass.getEnumConstants()) {
            names.add(((Enum<?>) constant).name());
        }

        names.sort(String::compareTo);
        for (String name : names) {
            enumValues.add(name);
        }
        return schema;
    }

    private ObjectNode typeOnlySchema(String typeName) {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", typeName);
        return schema;
    }

    private JavaType normalize(JavaType javaType) {
        return javaType == null
                ? objectMapper.getTypeFactory().constructType(Object.class)
                : javaType;
    }

    private boolean isBooleanType(Class<?> rawClass) {
        return boolean.class.equals(rawClass) || Boolean.class.equals(rawClass);
    }

    private boolean isIntegerType(Class<?> rawClass) {
        return byte.class.equals(rawClass)
                || Byte.class.equals(rawClass)
                || short.class.equals(rawClass)
                || Short.class.equals(rawClass)
                || int.class.equals(rawClass)
                || Integer.class.equals(rawClass)
                || long.class.equals(rawClass)
                || Long.class.equals(rawClass)
                || BigInteger.class.equals(rawClass);
    }

    private boolean isNumberType(Class<?> rawClass) {
        return float.class.equals(rawClass)
                || Float.class.equals(rawClass)
                || double.class.equals(rawClass)
                || Double.class.equals(rawClass)
                || BigDecimal.class.equals(rawClass);
    }

    private boolean isStringType(Class<?> rawClass) {
        return char.class.equals(rawClass)
                || Character.class.equals(rawClass)
                || String.class.equals(rawClass)
                || CharSequence.class.isAssignableFrom(rawClass);
    }

    private boolean isStringMapKey(Class<?> keyClass) {
        return String.class.equals(keyClass)
                || CharSequence.class.isAssignableFrom(keyClass)
                || Object.class.equals(keyClass);
    }

    private static <T> T requireValue(T value, String fieldName) {
        if (value != null) {
            return value;
        }

        throw new IllegalArgumentException(fieldName + " must not be null");
    }
}

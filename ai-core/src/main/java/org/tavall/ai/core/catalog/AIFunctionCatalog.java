package org.tavall.ai.core.catalog;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tavall.ai.core.annotation.AIParam;
import org.tavall.ai.core.annotation.AIFunction;
import org.tavall.ai.core.audit.AIFunctionAuditLogger;
import org.tavall.ai.core.audit.NoOpAIFunctionAuditLogger;
import org.tavall.ai.core.invocation.AIFunctionInvocationResult;
import org.tavall.ai.core.policy.AIFunctionInvocationContext;
import org.tavall.ai.core.policy.AIFunctionPolicy;
import org.tavall.ai.core.policy.AllowAllAIFunctionPolicy;
import org.tavall.ai.core.schema.AIFunctionSchemaGenerator;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class AIFunctionCatalog {
    private static final Logger LOGGER = LoggerFactory.getLogger(AIFunctionCatalog.class);

    private final ObjectMapper objectMapper;
    private final AIFunctionSchemaGenerator schemaGenerator;
    private final AIFunctionPolicy functionPolicy;
    private final AIFunctionAuditLogger auditLogger;
    private final AIFunctionScanner functionScanner;
    private final Map<String, RegisteredFunction> functions;
    private Path stateFilePath;
    private Path snapshotFilePath;
    private FileTime lastObservedStateWriteTime;

    public AIFunctionCatalog(ObjectMapper objectMapper) {
        this(
                objectMapper,
                new AIFunctionSchemaGenerator(requireValue(objectMapper, "objectMapper")),
                new AllowAllAIFunctionPolicy(),
                new NoOpAIFunctionAuditLogger(),
                new AIFunctionScanner()
        );
    }

    public AIFunctionCatalog(
            ObjectMapper objectMapper,
            AIFunctionSchemaGenerator schemaGenerator,
            AIFunctionPolicy functionPolicy,
            AIFunctionAuditLogger auditLogger
    ) {
        this(objectMapper, schemaGenerator, functionPolicy, auditLogger, new AIFunctionScanner());
    }

    public AIFunctionCatalog(
            ObjectMapper objectMapper,
            AIFunctionSchemaGenerator schemaGenerator,
            AIFunctionPolicy functionPolicy,
            AIFunctionAuditLogger auditLogger,
            AIFunctionScanner functionScanner
    ) {
        this.objectMapper = requireValue(objectMapper, "objectMapper");
        this.schemaGenerator = requireValue(schemaGenerator, "schemaGenerator");
        this.functionPolicy = requireValue(functionPolicy, "functionPolicy");
        this.auditLogger = requireValue(auditLogger, "auditLogger");
        this.functionScanner = requireValue(functionScanner, "functionScanner");
        this.functions = new LinkedHashMap<>();
    }

    public synchronized AIFunctionCatalog configureStateFiles(Path stateFilePath, Path snapshotFilePath) {
        this.stateFilePath = normalizePath(stateFilePath);
        this.snapshotFilePath = normalizePath(snapshotFilePath);
        refreshStateFromDisk(true);
        writeSnapshotIfConfigured();
        return this;
    }

    public synchronized Path getStateFilePath() {
        return stateFilePath;
    }

    public synchronized Path getSnapshotFilePath() {
        return snapshotFilePath;
    }

    public synchronized void registerInstances(Iterable<?> instances) {
        Iterable<?> safeInstances = requireValue(instances, "instances");
        for (Object instance : safeInstances) {
            registerInstance(requireValue(instance, "instance"), AIFunctionRegistrationSource.MANUAL);
        }
        synchronizeStateAndSnapshot(true);
    }

    public synchronized void registerInstances(Object... instances) {
        registerInstances(List.of(requireValue(instances, "instances")));
    }

    public synchronized void registerRegistrars(Iterable<? extends AIFunctionRegistrar> registrars) {
        Iterable<? extends AIFunctionRegistrar> safeRegistrars = requireValue(registrars, "registrars");
        for (AIFunctionRegistrar registrar : safeRegistrars) {
            requireValue(registrar, "registrar").register(this);
        }
    }

    public synchronized void scanPackages(Iterable<String> packageNames) {
        Iterable<String> safePackageNames = requireValue(packageNames, "packageNames");
        for (Class<?> candidateClass : functionScanner.scanPackages(safePackageNames)) {
            registerScannedClass(candidateClass);
        }
        synchronizeStateAndSnapshot(true);
    }

    public synchronized void scanPackages(String... packageNames) {
        scanPackages(List.of(requireValue(packageNames, "packageNames")));
    }

    public synchronized Map<String, AIFunctionDefinition> getFunctionDefinitions() {
        refreshStateFromDiskIfChanged();
        List<String> names = new ArrayList<>(functions.keySet());
        names.sort(String::compareTo);

        Map<String, AIFunctionDefinition> result = new LinkedHashMap<>();
        for (String name : names) {
            result.put(name, functions.get(name).toDefinition());
        }
        return Collections.unmodifiableMap(result);
    }

    @Deprecated
    public synchronized Map<String, AIFunctionDefinition> getToolDefinitions() {
        return getFunctionDefinitions();
    }

    public synchronized ArrayNode exportCanonicalFunctionSchemas() {
        ArrayNode schemas = objectMapper.createArrayNode();
        for (AIFunctionDefinition definition : getFunctionDefinitions().values()) {
            ObjectNode schema = objectMapper.createObjectNode();
            schema.put("name", definition.getName());
            schema.put("description", definition.getDescription());
            schema.put("signature", definition.getSignature());
            schema.put("enabled", definition.isEnabled());
            schema.put("registrationSource", definition.getRegistrationSource().getWireValue());
            schema.set("parameters", definition.getCanonicalParametersSchema());

            ArrayNode required = schema.putArray("required");
            for (String requiredParameter : definition.getRequiredParameters()) {
                required.add(requiredParameter);
            }
            schemas.add(schema);
        }
        return schemas;
    }

    @Deprecated
    public synchronized ArrayNode exportCanonicalToolSchemas() {
        return exportCanonicalFunctionSchemas();
    }

    public synchronized AIFunctionInvocationResult invokeResult(String functionName, JsonNode argumentsJson) {
        return invokeResult(null, functionName, argumentsJson);
    }

    public synchronized AIFunctionInvocationResult invokeResult(String callId, String functionName, JsonNode argumentsJson) {
        String safeFunctionName = requireText(functionName, "functionName");
        JsonNode safeArguments = normalizeArguments(argumentsJson);
        refreshStateFromDiskIfChanged();

        RegisteredFunction function = functions.get(safeFunctionName);
        if (function == null) {
            return buildFailure(callId, safeFunctionName, safeArguments, "not_found", "No function registered with name: " + safeFunctionName);
        }

        if (!function.enabled) {
            return buildFailure(callId, safeFunctionName, safeArguments, "disabled", "Function '" + safeFunctionName + "' is disabled.");
        }

        int argumentSize = estimateArgumentSize(safeArguments);
        auditLogger.onInvocationStart(safeFunctionName, argumentSize);
        try {
            functionPolicy.checkInvocation(new AIFunctionInvocationContext(safeFunctionName, safeArguments, function.toDefinition()));
            Object[] invocationArguments = mapArguments(function, safeArguments);
            Object result = function.method.invoke(function.target, invocationArguments);
            auditLogger.onInvocationSuccess(safeFunctionName, argumentSize);
            JsonNode payload = objectMapper.valueToTree(result);
            return new AIFunctionInvocationResult(callId, safeFunctionName, safeArguments, true, "", "", payload);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getTargetException() != null ? exception.getTargetException() : exception;
            auditLogger.onInvocationFailure(safeFunctionName, argumentSize, cause);
            return buildFailure(callId, safeFunctionName, safeArguments, "invocation_error", messageFor(cause, "Function invocation failed."));
        } catch (IllegalArgumentException exception) {
            auditLogger.onInvocationFailure(safeFunctionName, argumentSize, exception);
            return buildFailure(callId, safeFunctionName, safeArguments, "invalid_arguments", messageFor(exception, "Invalid function arguments."));
        } catch (Exception exception) {
            auditLogger.onInvocationFailure(safeFunctionName, argumentSize, exception);
            return buildFailure(callId, safeFunctionName, safeArguments, "invocation_error", messageFor(exception, "Function invocation failed."));
        }
    }

    public synchronized Object invoke(String functionName, String argumentsJson) {
        String safeFunctionName = requireText(functionName, "functionName");
        String safeArgumentsJson = requireValue(argumentsJson, "argumentsJson");
        try {
            return invoke(safeFunctionName, objectMapper.readTree(safeArgumentsJson));
        } catch (IOException exception) {
            throw new IllegalArgumentException("Invalid function arguments JSON for " + safeFunctionName, exception);
        }
    }

    public synchronized Object invoke(String functionName, JsonNode argumentsJson) {
        AIFunctionInvocationResult result = invokeResult(functionName, argumentsJson);
        if (!result.isSuccess()) {
            throw new IllegalStateException(result.getErrorMessage());
        }

        AIFunctionDefinition definition = lookupDefinition(functionName);
        if (void.class.equals(definition.getMethod().getReturnType())) {
            return null;
        }

        JavaType returnType = objectMapper.getTypeFactory().constructType(definition.getMethod().getGenericReturnType());
        return objectMapper.convertValue(result.getPayload(), returnType);
    }

    public synchronized JsonNode invokeAsJson(String functionName, JsonNode argumentsJson) {
        AIFunctionInvocationResult result = invokeResult(functionName, argumentsJson);
        return result.getPayload();
    }

    public synchronized JsonNode buildSnapshot() {
        refreshStateFromDiskIfChanged();
        return buildSnapshotDocument();
    }

    public synchronized void writeSnapshot() {
        writeSnapshotIfConfigured();
    }

    public synchronized void reloadStateFromDisk() {
        refreshStateFromDisk(true);
    }

    @Deprecated
    public synchronized void registerTargets(Iterable<?> targets) {
        registerInstances(targets);
    }

    private void registerInstance(Object target, AIFunctionRegistrationSource registrationSource) {
        Class<?> targetType = target.getClass();
        Map<String, AIFunctionSettingsDescriptor> settingsByFunction = settingsByFunction(target);
        for (DiscoveredMethod discoveredMethod : collectAnnotatedMethods(targetType)) {
            registerFunction(buildRegisteredFunction(
                    targetType,
                    target,
                    discoveredMethod,
                    registrationSource,
                    settingsByFunction.get(resolveFunctionName(targetType, discoveredMethod.method(), discoveredMethod.annotation()))
            ));
        }
    }

    private void registerScannedClass(Class<?> candidateClass) {
        List<DiscoveredMethod> annotatedMethods = collectAnnotatedMethods(candidateClass);
        if (annotatedMethods.isEmpty()) {
            return;
        }

        boolean needsInstance = annotatedMethods.stream().anyMatch(item -> !Modifier.isStatic(item.method().getModifiers()));
        Object target = needsInstance ? instantiateScannedClass(candidateClass) : null;
        Map<String, AIFunctionSettingsDescriptor> settingsByFunction = settingsByFunction(target);
        for (DiscoveredMethod discoveredMethod : annotatedMethods) {
            registerFunction(buildRegisteredFunction(
                    candidateClass,
                    target,
                    discoveredMethod,
                    AIFunctionRegistrationSource.SCAN,
                    settingsByFunction.get(resolveFunctionName(candidateClass, discoveredMethod.method(), discoveredMethod.annotation()))
            ));
        }
    }

    private Object instantiateScannedClass(Class<?> candidateClass) {
        int modifiers = candidateClass.getModifiers();
        if (candidateClass.isInterface() || Modifier.isAbstract(modifiers)) {
            throw new IllegalStateException("Cannot instantiate abstract function owner: " + candidateClass.getName());
        }
        if (candidateClass.isMemberClass() && !Modifier.isStatic(modifiers)) {
            throw new IllegalStateException("Cannot instantiate non-static inner function owner: " + candidateClass.getName());
        }

        try {
            Constructor<?> constructor = candidateClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (NoSuchMethodException exception) {
            throw new IllegalStateException("Scanned function owner requires an accessible zero-arg constructor: " + candidateClass.getName(), exception);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getTargetException() != null ? exception.getTargetException() : exception;
            throw new IllegalStateException("Failed to instantiate scanned function owner: " + candidateClass.getName(), cause);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to instantiate scanned function owner: " + candidateClass.getName(), exception);
        }
    }

    private RegisteredFunction buildRegisteredFunction(
            Class<?> ownerType,
            Object target,
            DiscoveredMethod discoveredMethod,
            AIFunctionRegistrationSource registrationSource,
            AIFunctionSettingsDescriptor settingsDescriptor
    ) {
        Method method = discoveredMethod.method();
        method.setAccessible(true);
        AIFunction annotation = discoveredMethod.annotation();
        String functionName = resolveFunctionName(ownerType, method, annotation);
        String description = annotation.description() == null ? "" : annotation.description().trim();
        List<AIFunctionParameterDefinition> parameterDefinitions = buildParameterDefinitions(method);
        List<String> requiredParameters = new ArrayList<>();
        for (AIFunctionParameterDefinition parameterDefinition : parameterDefinitions) {
            if (parameterDefinition.isRequired()) {
                requiredParameters.add(parameterDefinition.getName());
            }
        }

        ObjectNode settingsSchema = null;
        JsonNode currentSettings = null;
        JsonNode defaultSettings = null;
        String settingsDescription = "";
        if (settingsDescriptor != null) {
            settingsSchema = schemaGenerator.generateTypeSchema(settingsDescriptor.getSettingsType());
            currentSettings = objectMapper.valueToTree(settingsDescriptor.getCurrentSettings());
            defaultSettings = objectMapper.valueToTree(settingsDescriptor.getDefaultSettings());
            if (currentSettings == null || currentSettings.isNull()) {
                currentSettings = defaultSettings == null ? objectMapper.nullNode() : defaultSettings.deepCopy();
            }
            if (defaultSettings == null) {
                defaultSettings = objectMapper.nullNode();
            }
            settingsDescription = settingsDescriptor.getDescription();
        }

        return new RegisteredFunction(
                functionName,
                description,
                buildSignature(ownerType, method, parameterDefinitions),
                parameterDefinitions,
                requiredParameters,
                schemaGenerator.generateInputSchema(parameterDefinitions),
                method,
                target,
                ownerType,
                registrationSource,
                settingsSchema,
                currentSettings,
                defaultSettings,
                settingsDescription,
                target instanceof AIFunctionSettingsContributor contributor ? contributor : null
        );
    }

    private void registerFunction(RegisteredFunction function) {
        RegisteredFunction existing = functions.get(function.name);
        if (existing == null) {
            functions.put(function.name, function);
            return;
        }

        if (existing.registrationSource == AIFunctionRegistrationSource.MANUAL
                && function.registrationSource == AIFunctionRegistrationSource.SCAN) {
            LOGGER.info("Skipping scanned function '{}' because a manual registration already exists.", function.name);
            return;
        }

        if (existing.registrationSource == AIFunctionRegistrationSource.SCAN
                && function.registrationSource == AIFunctionRegistrationSource.MANUAL) {
            LOGGER.info("Replacing scanned function '{}' with manual registration.", function.name);
            functions.put(function.name, function);
            return;
        }

        String message = "Duplicate function name '" + function.name + "' for " + function.signature
                + "; already registered by " + existing.signature;
        LOGGER.error(message);
        throw new IllegalStateException(message);
    }

    private List<DiscoveredMethod> collectAnnotatedMethods(Class<?> targetType) {
        LinkedHashMap<String, DiscoveredMethod> discovered = new LinkedHashMap<>();
        collectAnnotatedMethods(targetType, targetType, discovered, new LinkedHashSet<>());
        List<DiscoveredMethod> result = new ArrayList<>(discovered.values());
        result.sort(Comparator.comparing(item -> methodSortKey(item.method())));
        return result;
    }

    private void collectAnnotatedMethods(
            Class<?> lookupType,
            Class<?> targetType,
            Map<String, DiscoveredMethod> discovered,
            Set<Class<?>> visitedTypes
    ) {
        if (lookupType == null || !visitedTypes.add(lookupType) || Object.class.equals(lookupType)) {
            return;
        }

        for (Method candidate : lookupType.getDeclaredMethods()) {
            if (candidate.isSynthetic() || candidate.isBridge()) {
                continue;
            }

            AIFunction annotation = candidate.getAnnotation(AIFunction.class);
            if (annotation == null) {
                continue;
            }

            Method invokable = resolveInvokableMethod(targetType, candidate);
            discovered.putIfAbsent(signatureKey(invokable), new DiscoveredMethod(invokable, annotation));
        }

        for (Class<?> interfaceType : lookupType.getInterfaces()) {
            collectAnnotatedMethods(interfaceType, targetType, discovered, visitedTypes);
        }

        collectAnnotatedMethods(lookupType.getSuperclass(), targetType, discovered, visitedTypes);
    }

    private Method resolveInvokableMethod(Class<?> targetType, Method candidate) {
        Method resolved = findMethodInHierarchy(targetType, candidate.getName(), candidate.getParameterTypes());
        return resolved == null ? candidate : resolved;
    }

    private Method findMethodInHierarchy(Class<?> targetType, String name, Class<?>[] parameterTypes) {
        Class<?> current = targetType;
        while (current != null && !Object.class.equals(current)) {
            try {
                Method method = current.getDeclaredMethod(name, parameterTypes);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException ignored) {
                Method interfaceMethod = findMethodInInterfaces(current.getInterfaces(), name, parameterTypes);
                if (interfaceMethod != null) {
                    interfaceMethod.setAccessible(true);
                    return interfaceMethod;
                }
                current = current.getSuperclass();
            }
        }

        return null;
    }

    private Method findMethodInInterfaces(Class<?>[] interfaceTypes, String name, Class<?>[] parameterTypes) {
        for (Class<?> interfaceType : interfaceTypes) {
            Method method = findMethodInInterfaceHierarchy(interfaceType, name, parameterTypes);
            if (method != null) {
                return method;
            }
        }
        return null;
    }

    private Method findMethodInInterfaceHierarchy(Class<?> interfaceType, String name, Class<?>[] parameterTypes) {
        try {
            return interfaceType.getDeclaredMethod(name, parameterTypes);
        } catch (NoSuchMethodException ignored) {
            for (Class<?> parentInterface : interfaceType.getInterfaces()) {
                Method method = findMethodInInterfaceHierarchy(parentInterface, name, parameterTypes);
                if (method != null) {
                    return method;
                }
            }
            return null;
        }
    }

    private List<AIFunctionParameterDefinition> buildParameterDefinitions(Method method) {
        Parameter[] parameters = method.getParameters();
        List<AIFunctionParameterDefinition> definitions = new ArrayList<>();
        Set<String> names = new LinkedHashSet<>();
        for (int index = 0; index < parameters.length; index++) {
            Parameter parameter = parameters[index];
            AIParam annotation = parameter.getAnnotation(AIParam.class);
            String parameterName = resolveParameterName(annotation, parameter, method);
            if (!names.add(parameterName)) {
                throw new IllegalStateException("Duplicate parameter name '" + parameterName + "' in method " + method);
            }

            String description = annotation == null || annotation.description() == null ? "" : annotation.description().trim();
            boolean required = resolveRequired(annotation, parameter);
            JavaType javaType = objectMapper.getTypeFactory().constructType(parameter.getParameterizedType());
            definitions.add(new AIFunctionParameterDefinition(index, parameterName, description, javaType, required));
        }
        return definitions;
    }

    private String resolveParameterName(AIParam annotation, Parameter parameter, Method method) {
        if (annotation != null && annotation.name() != null && !annotation.name().isBlank()) {
            return annotation.name().trim();
        }

        if (parameter.isNamePresent() && parameter.getName() != null && !parameter.getName().isBlank()) {
            return parameter.getName();
        }

        throw new IllegalStateException(
                "Parameter names are unavailable for method " + method
                        + ". Compile with -parameters or annotate the parameter with @AIParam(name=...)."
        );
    }

    private boolean resolveRequired(AIParam annotation, Parameter parameter) {
        if (annotation != null) {
            return annotation.required();
        }
        return !Optional.class.isAssignableFrom(parameter.getType());
    }

    private String resolveFunctionName(Class<?> ownerType, Method method, AIFunction annotation) {
        if (annotation != null && annotation.name() != null && !annotation.name().isBlank()) {
            return annotation.name().trim();
        }
        return ownerType.getSimpleName() + "_" + method.getName();
    }

    private String buildSignature(Class<?> ownerType, Method method, List<AIFunctionParameterDefinition> parameters) {
        StringBuilder signature = new StringBuilder();
        signature.append(ownerType.getSimpleName()).append('#').append(method.getName()).append('(');
        for (int index = 0; index < parameters.size(); index++) {
            AIFunctionParameterDefinition parameterDefinition = parameters.get(index);
            if (index > 0) {
                signature.append(", ");
            }
            signature.append(parameterDefinition.getJavaType().toCanonical())
                    .append(' ')
                    .append(parameterDefinition.getName());
        }
        signature.append(')');
        return signature.toString();
    }

    private String methodSortKey(Method method) {
        StringBuilder builder = new StringBuilder();
        builder.append(method.getDeclaringClass().getName()).append('#').append(method.getName()).append('(');
        Class<?>[] parameterTypes = method.getParameterTypes();
        for (int index = 0; index < parameterTypes.length; index++) {
            if (index > 0) {
                builder.append(',');
            }
            builder.append(parameterTypes[index].getName());
        }
        builder.append(')');
        return builder.toString();
    }

    private String signatureKey(Method method) {
        return method.getName() + "::" + java.util.Arrays.stream(method.getParameterTypes()).map(Class::getName).reduce((left, right) -> left + "," + right).orElse("");
    }

    private AIFunctionDefinition lookupDefinition(String functionName) {
        RegisteredFunction function = functions.get(functionName);
        if (function != null) {
            return function.toDefinition();
        }
        throw new IllegalArgumentException("No function registered with name: " + functionName);
    }

    private JsonNode normalizeArguments(JsonNode argumentsJson) {
        if (argumentsJson == null) {
            return objectMapper.createObjectNode();
        }
        if (argumentsJson.isObject()) {
            return argumentsJson.deepCopy();
        }
        throw new IllegalArgumentException("Function arguments must be a JSON object.");
    }

    private int estimateArgumentSize(JsonNode argumentsJson) {
        try {
            return objectMapper.writeValueAsBytes(argumentsJson).length;
        } catch (Exception ignored) {
            return -1;
        }
    }

    private Object[] mapArguments(RegisteredFunction function, JsonNode argumentsJson) {
        ObjectNode argumentsObject = (ObjectNode) argumentsJson;
        Object[] invocationArguments = new Object[function.parameters.size()];
        for (AIFunctionParameterDefinition parameterDefinition : function.parameters) {
            JsonNode rawValue = argumentsObject.get(parameterDefinition.getName());
            if (rawValue != null && !rawValue.isNull()) {
                invocationArguments[parameterDefinition.getIndex()] = convertValue(function, parameterDefinition, rawValue);
                continue;
            }

            if (!parameterDefinition.isRequired()) {
                invocationArguments[parameterDefinition.getIndex()] = defaultValue(parameterDefinition);
                continue;
            }

            throw new IllegalArgumentException(
                    "Missing required parameter '" + parameterDefinition.getName() + "' for function " + function.name
            );
        }
        return invocationArguments;
    }

    private Object defaultValue(AIFunctionParameterDefinition parameterDefinition) {
        if (Optional.class.isAssignableFrom(parameterDefinition.getJavaType().getRawClass())) {
            return Optional.empty();
        }
        return null;
    }

    private Object convertValue(
            RegisteredFunction function,
            AIFunctionParameterDefinition parameterDefinition,
            JsonNode rawValue
    ) {
        try {
            JavaType javaType = parameterDefinition.getJavaType();
            if (Optional.class.isAssignableFrom(javaType.getRawClass())) {
                JavaType contentType = javaType.containedTypeCount() > 0
                        ? javaType.containedType(0)
                        : objectMapper.getTypeFactory().constructType(Object.class);
                Object convertedContent = objectMapper.convertValue(rawValue, contentType);
                return Optional.ofNullable(convertedContent);
            }

            Object converted = objectMapper.convertValue(rawValue, javaType);
            if (converted != null || !javaType.isPrimitive()) {
                return converted;
            }
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "Failed to map parameter '" + parameterDefinition.getName() + "' for function " + function.name,
                    exception
            );
        }

        throw new IllegalArgumentException(
                "Parameter '" + parameterDefinition.getName() + "' cannot be null for function " + function.name
        );
    }

    private Map<String, AIFunctionSettingsDescriptor> settingsByFunction(Object target) {
        if (!(target instanceof AIFunctionSettingsContributor contributor)) {
            return Map.of();
        }

        Map<String, AIFunctionSettingsDescriptor> descriptors = new LinkedHashMap<>();
        for (AIFunctionSettingsDescriptor descriptor : contributor.describeFunctionSettings()) {
            AIFunctionSettingsDescriptor safeDescriptor = requireValue(descriptor, "descriptor");
            String functionName = safeDescriptor.getFunctionName();
            if (descriptors.putIfAbsent(functionName, safeDescriptor) != null) {
                throw new IllegalStateException("Duplicate settings descriptor for function '" + functionName + "'.");
            }
        }
        return descriptors;
    }

    private void synchronizeStateAndSnapshot(boolean forceReloadState) {
        if (stateFilePath != null) {
            refreshStateFromDisk(forceReloadState);
            persistStateDocument();
        }
        writeSnapshotIfConfigured();
    }

    private void refreshStateFromDiskIfChanged() {
        refreshStateFromDisk(false);
    }

    private void refreshStateFromDisk(boolean forceReload) {
        if (stateFilePath == null) {
            return;
        }
        ensureParentDirectory(stateFilePath);
        if (!Files.exists(stateFilePath)) {
            if (lastObservedStateWriteTime != null) {
                resetRuntimeStateToDefaults();
                lastObservedStateWriteTime = null;
            }
            return;
        }

        try {
            FileTime modifiedTime = Files.getLastModifiedTime(stateFilePath);
            if (!forceReload && Objects.equals(modifiedTime, lastObservedStateWriteTime)) {
                return;
            }

            JsonNode root;
            try (var reader = Files.newBufferedReader(stateFilePath)) {
                root = objectMapper.readTree(reader);
            }
            applyStateDocument(root);
            lastObservedStateWriteTime = modifiedTime;
            writeSnapshotIfConfigured();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read function state file: " + stateFilePath, exception);
        }
    }

    private void resetRuntimeStateToDefaults() {
        for (RegisteredFunction function : functions.values()) {
            function.resetToDefaults();
        }
    }

    private void applyStateDocument(JsonNode root) {
        resetRuntimeStateToDefaults();
        if (root == null || !root.isObject()) {
            return;
        }

        JsonNode functionsNode = root.path("functions");
        if (!functionsNode.isObject()) {
            return;
        }

        for (Map.Entry<String, JsonNode> entry : iterable(functionsNode.fields())) {
            RegisteredFunction function = functions.get(entry.getKey());
            if (function == null || !entry.getValue().isObject()) {
                continue;
            }

            JsonNode functionState = entry.getValue();
            if (functionState.has("enabled")) {
                function.enabled = functionState.path("enabled").asBoolean(true);
            }

            JsonNode settingsNode = functionState.get("settings");
            if (settingsNode != null && function.hasSettings()) {
                function.applySettings(settingsNode, objectMapper);
            }
        }
    }

    private void persistStateDocument() {
        if (stateFilePath == null) {
            return;
        }
        ensureParentDirectory(stateFilePath);
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(stateFilePath.toFile(), buildStateDocument());
            lastObservedStateWriteTime = Files.getLastModifiedTime(stateFilePath);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to write function state file: " + stateFilePath, exception);
        }
    }

    private ObjectNode buildStateDocument() {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("updatedAt", Instant.now().toString());
        ObjectNode functionsNode = root.putObject("functions");
        List<String> names = new ArrayList<>(functions.keySet());
        names.sort(String::compareTo);
        for (String name : names) {
            RegisteredFunction function = functions.get(name);
            ObjectNode functionNode = functionsNode.putObject(name);
            functionNode.put("enabled", function.enabled);
            if (function.hasSettings() && function.currentSettings != null) {
                functionNode.set("settings", function.currentSettings.deepCopy());
            }
            functionNode.put("updatedAt", function.updatedAt);
        }
        return root;
    }

    private JsonNode buildSnapshotDocument() {
        ObjectNode root = objectMapper.createObjectNode();
        ObjectNode summary = root.putObject("summary");
        int totalFunctions = functions.size();
        int enabledFunctions = (int) functions.values().stream().filter(function -> function.enabled).count();
        summary.put("totalFunctions", totalFunctions);
        summary.put("enabledFunctions", enabledFunctions);
        summary.put("disabledFunctions", totalFunctions - enabledFunctions);
        summary.put("lastUpdated", Instant.now().toString());
        summary.put("stateFilePath", stateFilePath == null ? "" : stateFilePath.toString());
        summary.put("snapshotFilePath", snapshotFilePath == null ? "" : snapshotFilePath.toString());

        ArrayNode functionArray = root.putArray("functions");
        List<String> names = new ArrayList<>(functions.keySet());
        names.sort(String::compareTo);
        for (String name : names) {
            RegisteredFunction function = functions.get(name);
            ObjectNode item = functionArray.addObject();
            item.put("name", function.name);
            item.put("description", function.description);
            item.put("signature", function.signature);
            item.put("registrationSource", function.registrationSource.getWireValue());
            item.put("enabled", function.enabled);
            item.put("hasSettings", function.hasSettings());
            item.put("settingsDescription", function.settingsDescription);
            item.put("updatedAt", function.updatedAt);
            item.set("parametersSchema", function.canonicalParametersSchema.deepCopy());

            ArrayNode required = item.putArray("requiredParameters");
            for (String requiredParameter : function.requiredParameters) {
                required.add(requiredParameter);
            }

            if (function.hasSettings()) {
                item.set("settingsSchema", function.settingsSchema.deepCopy());
                item.set("settings", function.currentSettings == null ? objectMapper.nullNode() : function.currentSettings.deepCopy());
                item.set("defaultSettings", function.defaultSettings == null ? objectMapper.nullNode() : function.defaultSettings.deepCopy());
            }
        }
        return root;
    }

    private void writeSnapshotIfConfigured() {
        if (snapshotFilePath == null) {
            return;
        }
        ensureParentDirectory(snapshotFilePath);
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(snapshotFilePath.toFile(), buildSnapshotDocument());
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to write function snapshot file: " + snapshotFilePath, exception);
        }
    }

    private void ensureParentDirectory(Path path) {
        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to create directory for path: " + path, exception);
        }
    }

    private Path normalizePath(Path path) {
        return path == null ? null : path.toAbsolutePath().normalize();
    }

    private AIFunctionInvocationResult buildFailure(
            String callId,
            String functionName,
            JsonNode arguments,
            String errorCode,
            String errorMessage
    ) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("success", false);
        payload.put("functionName", functionName);
        ObjectNode error = payload.putObject("error");
        error.put("code", errorCode);
        error.put("message", errorMessage);
        return new AIFunctionInvocationResult(callId, functionName, arguments, false, errorCode, errorMessage, payload);
    }

    private String messageFor(Throwable throwable, String fallback) {
        if (throwable == null) {
            return fallback;
        }
        String message = throwable.getMessage();
        return message == null || message.isBlank() ? fallback : message;
    }

    private static <T> Iterable<T> iterable(java.util.Iterator<T> iterator) {
        return () -> iterator;
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

    private record DiscoveredMethod(Method method, AIFunction annotation) {
    }

    private final class RegisteredFunction {
        private final String name;
        private final String description;
        private final String signature;
        private final List<AIFunctionParameterDefinition> parameters;
        private final List<String> requiredParameters;
        private final ObjectNode canonicalParametersSchema;
        private final Method method;
        private final Object target;
        private final Class<?> ownerType;
        private final AIFunctionRegistrationSource registrationSource;
        private final ObjectNode settingsSchema;
        private final JsonNode defaultSettings;
        private final String settingsDescription;
        private final AIFunctionSettingsContributor settingsContributor;
        private boolean enabled;
        private JsonNode currentSettings;
        private String updatedAt;

        private RegisteredFunction(
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
                ObjectNode settingsSchema,
                JsonNode currentSettings,
                JsonNode defaultSettings,
                String settingsDescription,
                AIFunctionSettingsContributor settingsContributor
        ) {
            this.name = name;
            this.description = description;
            this.signature = signature;
            this.parameters = List.copyOf(parameters);
            this.requiredParameters = List.copyOf(requiredParameters);
            this.canonicalParametersSchema = canonicalParametersSchema.deepCopy();
            this.method = method;
            this.target = target;
            this.ownerType = ownerType;
            this.registrationSource = registrationSource;
            this.settingsSchema = settingsSchema == null ? null : settingsSchema.deepCopy();
            this.currentSettings = currentSettings == null ? null : currentSettings.deepCopy();
            this.defaultSettings = defaultSettings == null ? null : defaultSettings.deepCopy();
            this.settingsDescription = settingsDescription == null ? "" : settingsDescription;
            this.settingsContributor = settingsContributor;
            this.enabled = true;
            this.updatedAt = Instant.now().toString();
        }

        private boolean hasSettings() {
            return settingsSchema != null;
        }

        private void resetToDefaults() {
            this.enabled = true;
            if (hasSettings()) {
                this.currentSettings = defaultSettings == null ? objectMapper.nullNode() : defaultSettings.deepCopy();
                applySettings(this.currentSettings, objectMapper);
            }
            this.updatedAt = Instant.now().toString();
        }

        private void applySettings(JsonNode settings, ObjectMapper objectMapper) {
            if (!hasSettings()) {
                return;
            }
            this.currentSettings = settings == null ? objectMapper.nullNode() : settings.deepCopy();
            if (settingsContributor != null) {
                settingsContributor.applyFunctionSettings(name, this.currentSettings.deepCopy(), objectMapper);
            }
            this.updatedAt = Instant.now().toString();
        }

        private AIFunctionDefinition toDefinition() {
            return new AIFunctionDefinition(
                    name,
                    description,
                    signature,
                    parameters,
                    requiredParameters,
                    canonicalParametersSchema,
                    method,
                    target,
                    ownerType,
                    registrationSource,
                    enabled,
                    settingsSchema,
                    currentSettings,
                    defaultSettings,
                    settingsDescription,
                    updatedAt
            );
        }
    }
}

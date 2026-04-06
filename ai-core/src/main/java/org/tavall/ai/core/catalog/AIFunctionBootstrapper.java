package org.tavall.ai.core.catalog;

public final class AIFunctionBootstrapper {
    private final AIFunctionCatalog functionCatalog;

    public AIFunctionBootstrapper(AIFunctionCatalog functionCatalog) {
        this.functionCatalog = requireValue(functionCatalog, "functionCatalog");
    }

    public AIFunctionCatalog bootstrapInstances(Iterable<?> instances) {
        functionCatalog.registerInstances(instances);
        return functionCatalog;
    }

    public AIFunctionCatalog bootstrapRegistrars(Iterable<? extends AIFunctionRegistrar> registrars) {
        functionCatalog.registerRegistrars(registrars);
        return functionCatalog;
    }

    public AIFunctionCatalog scanPackages(Iterable<String> packageNames) {
        functionCatalog.scanPackages(packageNames);
        return functionCatalog;
    }

    public AIFunctionCatalog bootstrap(
            Iterable<? extends AIFunctionRegistrar> registrars,
            Iterable<String> packageNames
    ) {
        functionCatalog.registerRegistrars(registrars);
        functionCatalog.scanPackages(packageNames);
        return functionCatalog;
    }

    private static <T> T requireValue(T value, String fieldName) {
        if (value != null) {
            return value;
        }

        throw new IllegalArgumentException(fieldName + " must not be null");
    }
}

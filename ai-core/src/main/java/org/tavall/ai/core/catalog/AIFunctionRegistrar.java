package org.tavall.ai.core.catalog;

public interface AIFunctionRegistrar {
    Iterable<?> instances();

    default void register(AIFunctionCatalog functionCatalog) {
        functionCatalog.registerInstances(instances());
    }
}

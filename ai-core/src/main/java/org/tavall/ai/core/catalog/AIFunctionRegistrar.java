package org.tavall.ai.core.catalog;

/**
 * Contributes one or more service instances whose annotated methods should be published to an
 * {@link AIFunctionCatalog}.
 *
 * <p>The default registration path delegates the supplied instances to
 * {@link AIFunctionCatalog#registerInstances(Iterable)}, so normal catalog discovery, duplicate-name
 * handling, state synchronization, and function metadata rules remain centralized in the catalog.</p>
 */
public interface AIFunctionRegistrar {

    /**
     * Returns service instances that expose catalog functions.
     *
     * @return instances to inspect for function annotations; individual null elements are rejected
     *         by the catalog during registration
     */
    Iterable<?> instances();

    /**
     * Registers this contributor's instances with a function catalog.
     *
     * @param functionCatalog catalog that should receive the contributed instances
     * @throws NullPointerException if {@code functionCatalog} is {@code null}
     */
    default void register(AIFunctionCatalog functionCatalog) {
        functionCatalog.registerInstances(instances());
    }
}

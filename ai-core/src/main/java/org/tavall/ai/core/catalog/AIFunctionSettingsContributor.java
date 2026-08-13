package org.tavall.ai.core.catalog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Optional extension for function owners whose published functions expose configurable settings.
 *
 * <p>Descriptors advertise the settings shape associated with individual function names. When
 * catalog state supplies settings for one of those functions, the catalog calls
 * {@link #applyFunctionSettings(String, JsonNode, ObjectMapper)} so the owning service can validate
 * and materialize that configuration using the same JSON mapper as the catalog.</p>
 */
public interface AIFunctionSettingsContributor {

    /**
     * Describes settings supported by functions owned by this contributor.
     *
     * @return descriptors keyed logically by their declared function names; an empty iterable when
     *         the contributor exposes no configurable functions
     */
    Iterable<AIFunctionSettingsDescriptor> describeFunctionSettings();

    /**
     * Applies persisted or operator-supplied settings to one function owned by this contributor.
     *
     * @param functionName published function whose settings should be applied
     * @param settings JSON settings value associated with that function
     * @param objectMapper catalog mapper available for typed conversion and validation
     * @throws RuntimeException when the settings cannot be accepted or materialized; the catalog
     *                          treats such a failure as a configuration/application failure rather
     *                          than silently ignoring the settings
     */
    void applyFunctionSettings(String functionName, JsonNode settings, ObjectMapper objectMapper);
}

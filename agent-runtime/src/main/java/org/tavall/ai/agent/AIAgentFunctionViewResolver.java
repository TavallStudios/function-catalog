package org.tavall.ai.agent;

import org.tavall.ai.core.catalog.AIFunctionCatalog;
import org.tavall.ai.core.catalog.AIFunctionCatalogView;

/** Resolves the authoritative environment/job policy view for one agent execution. */
@FunctionalInterface
public interface AIAgentFunctionViewResolver {
    AIFunctionCatalogView resolve(
            AIFunctionCatalog catalog,
            AIAgentDefinition definition,
            AIAgentJob job
    );
}

package org.tavall.ai.agent;

import org.tavall.ai.core.catalog.AIFunctionCatalog;
import org.tavall.ai.core.catalog.AIFunctionCatalogView;

/**
 * Resolves the authoritative environment/job policy view for one agent execution.
 *
 * <p>The returned view establishes the functions visible before the runtime further narrows access
 * to the agent definition's requested function names and applies the execution budget's tool-call
 * limit. The view must be backed by the supplied catalog; the runtime rejects views created from a
 * different catalog.</p>
 */
@FunctionalInterface
public interface AIAgentFunctionViewResolver {

    /**
     * Resolves the function-catalog view permitted for one agent/job pair.
     *
     * @param catalog authoritative catalog being executed against
     * @param definition agent definition requesting provider and function access
     * @param job concrete job being executed
     * @return non-null policy view backed by {@code catalog}
     */
    AIFunctionCatalogView resolve(
            AIFunctionCatalog catalog,
            AIAgentDefinition definition,
            AIAgentJob job
    );
}

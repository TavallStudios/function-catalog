package org.tavall.ai.agent;

import org.tavall.ai.core.catalog.AIFunctionCatalogView;

import java.util.Objects;

/** Immutable execution request handed to a provider implementation. */
public record AIAgentExecutionRequest(
        AIAgentDefinition definition,
        AIAgentJob job,
        AIAgentExecutionBudget budget,
        AIFunctionCatalogView functionView
) {
    public AIAgentExecutionRequest {
        definition = Objects.requireNonNull(definition, "definition");
        job = Objects.requireNonNull(job, "job");
        budget = Objects.requireNonNull(budget, "budget");
        functionView = Objects.requireNonNull(functionView, "functionView");
    }
}

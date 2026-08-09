package org.tavall.ai.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import java.util.Objects;

/** Structured provider result returned to the Tavall agent runtime. */
public record AIAgentExecutionResult(
        AIAgentExecutionStatus status,
        JsonNode output,
        int toolCalls,
        int delegatedTasks,
        String errorMessage
) {
    public AIAgentExecutionResult {
        status = Objects.requireNonNull(status, "status");
        output = output == null ? JsonNodeFactory.instance.objectNode() : output.deepCopy();
        if (toolCalls < 0) throw new IllegalArgumentException("toolCalls must be >= 0");
        if (delegatedTasks < 0) throw new IllegalArgumentException("delegatedTasks must be >= 0");
        errorMessage = errorMessage == null ? "" : errorMessage;
    }

    @Override
    public JsonNode output() { return output.deepCopy(); }
}

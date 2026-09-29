package org.tavall.ai.cloud;

/** Stable result envelope. Provider-specific details remain behind Tavall Cloud CONTROL. */
public record TavallCloudOperationResult(
        boolean accepted,
        String operationId,
        String state,
        String evidenceHandle,
        String message
) {
}

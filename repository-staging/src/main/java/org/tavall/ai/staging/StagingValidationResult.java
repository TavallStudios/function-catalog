package org.tavall.ai.staging;

import java.util.List;

public record StagingValidationResult(boolean valid, List<StagingTopologyFinding> findings) {
    public StagingValidationResult {
        findings = List.copyOf(findings);
    }
}

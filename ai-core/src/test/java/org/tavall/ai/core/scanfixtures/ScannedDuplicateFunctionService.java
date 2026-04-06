package org.tavall.ai.core.scanfixtures;

import org.tavall.ai.core.annotation.AIFunction;

public final class ScannedDuplicateFunctionService {
    @AIFunction(name = "shared_function", description = "Scanned duplicate")
    public String shared() {
        return "scan";
    }
}

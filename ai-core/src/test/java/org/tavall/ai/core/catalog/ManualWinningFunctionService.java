package org.tavall.ai.core.catalog;

import org.tavall.ai.core.annotation.AIFunction;

public final class ManualWinningFunctionService {
    @AIFunction(name = "shared_function", description = "Manual duplicate that should win")
    public String shared() {
        return "manual";
    }
}

package org.tavall.ai.core.scanfixtures;

import org.tavall.ai.core.annotation.AIParam;
import org.tavall.ai.core.annotation.AIFunction;

public final class ScannedOnlyFunctionService {
    @AIFunction(name = "scan_only", description = "Discovered only through fallback scanning")
    private String discover(@AIParam(name = "name") String name) {
        return "scan:" + name;
    }
}

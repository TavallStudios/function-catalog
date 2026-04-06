package org.tavall.ai.mcp.server.scanfixtures;

import org.tavall.ai.core.annotation.AIParam;
import org.tavall.ai.core.annotation.AIFunction;

public final class ScannedEchoFunctionService {
    @AIFunction(name = "scan_echo", description = "Echoes scanned input")
    public String echo(@AIParam(name = "value") String value) {
        return "scan:" + value;
    }
}

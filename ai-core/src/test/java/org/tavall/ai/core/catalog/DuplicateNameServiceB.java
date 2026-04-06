package org.tavall.ai.core.catalog;

import org.tavall.ai.core.annotation.AIFunction;

public final class DuplicateNameServiceB {
    @AIFunction(name = "shared_tool")
    public String second() {
        return "b";
    }
}
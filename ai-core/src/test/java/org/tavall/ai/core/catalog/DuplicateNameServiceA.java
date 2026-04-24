package org.tavall.ai.core.catalog;

import org.tavall.ai.core.annotation.AIFunction;

public final class DuplicateNameServiceA {
    @AIFunction(name = "shared_tool")
    public String first() {
        return "a";
    }
}
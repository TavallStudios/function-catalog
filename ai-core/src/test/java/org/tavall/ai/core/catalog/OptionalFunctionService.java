package org.tavall.ai.core.catalog;

import org.tavall.ai.core.annotation.AIFunction;

import java.util.Optional;

public final class OptionalFunctionService {
    @AIFunction(name = "optional_prefix", description = "Returns the optional prefix or EMPTY")
    public String resolvePrefix(Optional<String> prefix) {
        return prefix.orElse("EMPTY");
    }
}

package org.tavall.ai.core.catalog;

import org.tavall.ai.core.annotation.AIParam;
import org.tavall.ai.core.annotation.AIFunction;

public final class PrivateFunctionService {
    @AIFunction(name = "private_join", description = "Joins two strings from a private method")
    private String join(
            @AIParam(name = "left") String left,
            @AIParam(name = "right") String right
    ) {
        return left + right;
    }
}

package org.tavall.ai.core.catalog;

import org.tavall.ai.core.annotation.AIParam;
import org.tavall.ai.core.annotation.AIFunction;

public final class ParamOverrideService {
    @AIFunction(name = "custom_tool", description = "Override param metadata")
    public OverrideResult execute(
            @AIParam(name = "user_id", description = "User identifier") String user,
            @AIParam(name = "optional_note", description = "Optional note", required = false) String note
    ) {
        return new OverrideResult(user, note == null ? "" : note);
    }
}
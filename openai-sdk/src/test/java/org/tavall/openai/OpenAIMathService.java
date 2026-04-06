package org.tavall.openai;

import org.tavall.ai.core.annotation.AIParam;
import org.tavall.ai.core.annotation.AIFunction;

public final class OpenAIMathService {
    @AIFunction(description = "Adds two numbers")
    public OpenAIMathResult add(
            @AIParam(name = "left") int left,
            @AIParam(name = "right") int right
    ) {
        return new OpenAIMathResult(left + right, left + "+" + right);
    }
}
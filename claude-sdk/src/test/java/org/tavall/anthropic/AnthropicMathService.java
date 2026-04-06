package org.tavall.anthropic;

import org.tavall.ai.core.annotation.AIParam;
import org.tavall.ai.core.annotation.AIFunction;

public final class AnthropicMathService {
    @AIFunction(description = "Multiplies two numbers")
    public AnthropicMathResult multiply(
            @AIParam(name = "left") int left,
            @AIParam(name = "right") int right
    ) {
        return new AnthropicMathResult(left * right, left + "*" + right);
    }
}
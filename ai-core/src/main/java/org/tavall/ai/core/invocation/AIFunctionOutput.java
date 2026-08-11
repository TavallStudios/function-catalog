package org.tavall.ai.core.invocation;

import java.util.List;

public record AIFunctionOutput(
        Object payload,
        List<AIFunctionImageContent> imageContents
) {
    public AIFunctionOutput {
        imageContents = imageContents == null ? List.of() : List.copyOf(imageContents);
    }

    public static AIFunctionOutput of(Object payload, AIFunctionImageContent... imageContents) {
        return new AIFunctionOutput(payload, List.of(imageContents));
    }
}

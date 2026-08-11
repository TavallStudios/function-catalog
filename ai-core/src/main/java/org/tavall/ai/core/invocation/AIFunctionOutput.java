package org.tavall.ai.core.invocation;

import java.util.List;

/**
 * Self-describing function result envelope carrying a structured payload plus rich content.
 *
 * <p>The envelope deliberately contains no provider-specific types. Transport adapters such as
 * MCP may project the rich content into their native result representation while preserving the
 * structured payload.</p>
 */
public final class AIFunctionOutput {
    public static final String OUTPUT_TYPE = "tavall.ai.function-output.v1";

    private final Object payload;
    private final List<AIFunctionContent> contents;

    public AIFunctionOutput(Object payload, List<? extends AIFunctionContent> contents) {
        this.payload = payload;
        this.contents = contents == null ? List.of() : List.copyOf(contents);
    }

    public static AIFunctionOutput of(Object payload, AIFunctionContent... contents) {
        return new AIFunctionOutput(payload, contents == null ? List.of() : List.of(contents));
    }

    public String getOutputType() {
        return OUTPUT_TYPE;
    }

    public Object getPayload() {
        return payload;
    }

    public List<AIFunctionContent> getContents() {
        return contents;
    }
}

package org.tavall.ai.core.contractfixtures;

import org.tavall.ai.core.annotation.AIFunction;

public interface AnnotatedFunctionContract {
    @AIFunction(name = "contract_only_function", description = "Contract-only function fixture")
    String execute();
}

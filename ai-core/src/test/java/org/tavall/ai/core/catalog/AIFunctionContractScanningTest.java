package org.tavall.ai.core.catalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

final class AIFunctionContractScanningTest {
    @Test
    void annotatedInterfacesDefineContractsWithoutBeingInstantiated() {
        AIFunctionCatalog catalog = new AIFunctionCatalog(new ObjectMapper());

        catalog.scanPackages("org.tavall.ai.core.contractfixtures");

        assertThat(catalog.getFunctionDefinitions()).isEmpty();
    }
}

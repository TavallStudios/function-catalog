package org.tavall.ai.mcp.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.tavall.ai.core.catalog.AIFunctionCatalog;
import org.tavall.ai.mcp.server.fixtures.NonceFunctionService;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AIFunctionMcpStandaloneHttpServerTest {
    @Test
    void shouldStartOnEphemeralPortWithNormalizedPathsAndCloseIdempotently() {
        AIFunctionCatalog catalog = new AIFunctionCatalog(new ObjectMapper().findAndRegisterModules());
        catalog.registerInstances(new NonceFunctionService("standalone-http"));
        AIFunctionMcpStandaloneHttpServer.Configuration configuration =
                new AIFunctionMcpStandaloneHttpServer.Configuration(
                        "127.0.0.1",
                        0,
                        "api/",
                        "mcp/",
                        "Function Catalog HTTP Test",
                        "1.0.0",
                        "test"
                );

        AIFunctionMcpStandaloneHttpServer server =
                AIFunctionMcpStandaloneHttpServer.start(catalog, configuration);
        try {
            assertTrue(server.port() > 0);
            assertEquals("/api/mcp", server.endpointPath());
            URI endpoint = server.localEndpointUri();
            assertEquals("127.0.0.1", endpoint.getHost());
            assertEquals(server.port(), endpoint.getPort());
            assertEquals("/api/mcp", endpoint.getPath());
        } finally {
            server.close();
            server.close();
        }
    }

    @Test
    void shouldRejectInvalidPublicHttpConfiguration() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new AIFunctionMcpStandaloneHttpServer.Configuration(
                        " ",
                        9000,
                        "",
                        "/mcp",
                        "test",
                        "1.0.0",
                        ""
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new AIFunctionMcpStandaloneHttpServer.Configuration(
                        "127.0.0.1",
                        65_536,
                        "",
                        "/mcp",
                        "test",
                        "1.0.0",
                        ""
                )
        );
    }
}

package org.tavall.ai.mcp.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures.SyncPromptSpecification;
import io.modelcontextprotocol.server.McpServerFeatures.SyncResourceSpecification;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import org.tavall.ai.core.catalog.AIFunctionCatalog;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

/** Reusable long-lived stdio MCP host for a canonical Function Catalog. */
public final class AIFunctionMcpStandaloneStdioServer implements AutoCloseable {
    private final McpSyncServer mcpServer;
    private final StdioServerTransportProvider transportProvider;
    private final CountDownLatch termination = new CountDownLatch(1);
    private final AtomicBoolean closed = new AtomicBoolean();

    private AIFunctionMcpStandaloneStdioServer(
            McpSyncServer mcpServer,
            StdioServerTransportProvider transportProvider
    ) {
        this.mcpServer = Objects.requireNonNull(mcpServer, "mcpServer");
        this.transportProvider = Objects.requireNonNull(transportProvider, "transportProvider");
    }

    public static AIFunctionMcpStandaloneStdioServer start(
            AIFunctionCatalog catalog,
            Configuration configuration,
            List<SyncResourceSpecification> resources,
            List<SyncPromptSpecification> prompts,
            Map<String, AIFunctionMcpToolPublisher.ToolPresentation> presentations
    ) {
        AIFunctionCatalog safeCatalog = Objects.requireNonNull(catalog, "catalog");
        Configuration safeConfiguration = Objects.requireNonNull(configuration, "configuration");
        List<SyncResourceSpecification> safeResources = List.copyOf(Objects.requireNonNull(resources, "resources"));
        List<SyncPromptSpecification> safePrompts = List.copyOf(Objects.requireNonNull(prompts, "prompts"));
        Map<String, AIFunctionMcpToolPublisher.ToolPresentation> safePresentations = Map.copyOf(
                Objects.requireNonNull(presentations, "presentations")
        );

        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        JacksonMcpJsonMapper jsonMapper = new JacksonMcpJsonMapper(objectMapper);
        StdioServerTransportProvider transportProvider = new StdioServerTransportProvider(jsonMapper);
        try {
            McpSyncServer server = McpServer.sync(transportProvider)
                    .serverInfo(safeConfiguration.serverName(), safeConfiguration.serverVersion())
                    .instructions(safeConfiguration.instructions())
                    .jsonMapper(jsonMapper)
                    .tools(new AIFunctionMcpToolPublisher(objectMapper).toolSpecifications(
                            safeCatalog,
                            safePresentations
                    ))
                    .resources(safeResources)
                    .prompts(safePrompts)
                    .build();
            return new AIFunctionMcpStandaloneStdioServer(server, transportProvider);
        } catch (RuntimeException exception) {
            try {
                transportProvider.close();
            } catch (RuntimeException closeFailure) {
                exception.addSuppressed(closeFailure);
            }
            throw exception;
        }
    }

    public static AIFunctionMcpStandaloneStdioServer start(
            AIFunctionCatalog catalog,
            Configuration configuration
    ) {
        return start(catalog, configuration, List.of(), List.of(), Map.of());
    }

    public void awaitTermination() throws InterruptedException {
        termination.await();
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        RuntimeException failure = null;
        try {
            mcpServer.close();
        } catch (RuntimeException exception) {
            failure = exception;
        }
        try {
            transportProvider.close();
        } catch (RuntimeException exception) {
            if (failure == null) {
                failure = exception;
            } else {
                failure.addSuppressed(exception);
            }
        } finally {
            termination.countDown();
        }
        if (failure != null) {
            throw failure;
        }
    }

    public record Configuration(
            String serverName,
            String serverVersion,
            String instructions
    ) {
        public Configuration {
            serverName = requireText(serverName, "serverName");
            serverVersion = requireText(serverVersion, "serverVersion");
            instructions = Objects.requireNonNullElse(instructions, "");
        }

        private static String requireText(String value, String fieldName) {
            if (value != null && !value.isBlank()) {
                return value;
            }
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }
}

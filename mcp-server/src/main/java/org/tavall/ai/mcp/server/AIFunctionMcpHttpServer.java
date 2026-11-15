package org.tavall.ai.mcp.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider;
import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.tavall.ai.core.catalog.AIFunctionCatalogView;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Loopback-only Streamable HTTP MCP host for an already-authorized Function Catalog view.
 *
 * <p>The caller owns the supplied view and its revocation lifecycle. This server never widens
 * the view, copies application behavior, or publishes functions outside that view.</p>
 */
public final class AIFunctionMcpHttpServer implements AutoCloseable {
    private final Path baseDirectory;
    private final String endpoint;
    private final Tomcat tomcat;
    private final McpSyncServer mcpServer;
    private final HttpServletStreamableServerTransportProvider transportProvider;
    private final AtomicBoolean closed = new AtomicBoolean();

    private AIFunctionMcpHttpServer(
            Path baseDirectory,
            String endpoint,
            Tomcat tomcat,
            McpSyncServer mcpServer,
            HttpServletStreamableServerTransportProvider transportProvider
    ) {
        this.baseDirectory = Objects.requireNonNull(baseDirectory, "baseDirectory");
        this.endpoint = Objects.requireNonNull(endpoint, "endpoint");
        this.tomcat = Objects.requireNonNull(tomcat, "tomcat");
        this.mcpServer = Objects.requireNonNull(mcpServer, "mcpServer");
        this.transportProvider = Objects.requireNonNull(transportProvider, "transportProvider");
    }

    /** Starts a loopback server on an ephemeral port and unguessable endpoint for the supplied authorized view. */
    public static AIFunctionMcpHttpServer start(AIFunctionCatalogView catalogView) {
        AIFunctionCatalogView safeCatalogView = Objects.requireNonNull(catalogView, "catalogView");
        String endpoint = "/mcp/" + UUID.randomUUID();
        ObjectMapper objectMapper = new ObjectMapper();
        McpJsonMapper jsonMapper = new JacksonMcpJsonMapper(objectMapper);
        AIFunctionMcpToolPublisher publisher = new AIFunctionMcpToolPublisher(objectMapper);

        HttpServletStreamableServerTransportProvider transportProvider =
                HttpServletStreamableServerTransportProvider.builder()
                        .jsonMapper(jsonMapper)
                        .mcpEndpoint(endpoint)
                        .build();

        McpSyncServer mcpServer = McpServer.sync(transportProvider)
                .serverInfo("Tavall Function Catalog View", "1.0.0")
                .instructions("Only the policy-filtered Tavall functions for this agent execution are available.")
                .jsonMapper(jsonMapper)
                .tools(publisher.viewToolSpecifications(safeCatalogView))
                .build();

        Path baseDirectory = createBaseDirectory();
        Tomcat tomcat = new Tomcat();
        tomcat.setBaseDir(baseDirectory.toString());
        tomcat.setPort(0);
        tomcat.getConnector();
        tomcat.getConnector().setProperty("address", "127.0.0.1");

        Context context = tomcat.addContext("", baseDirectory.toAbsolutePath().toString());
        Tomcat.addServlet(context, "tavallFunctionCatalogMcp", transportProvider);
        context.addServletMappingDecoded(endpoint, "tavallFunctionCatalogMcp");
        context.addServletMappingDecoded(endpoint + "/*", "tavallFunctionCatalogMcp");

        try {
            tomcat.start();
            return new AIFunctionMcpHttpServer(baseDirectory, endpoint, tomcat, mcpServer, transportProvider);
        } catch (Exception exception) {
            closeFailedStart(mcpServer, transportProvider, tomcat, baseDirectory);
            throw new IllegalStateException("Failed to start loopback Function Catalog MCP server.", exception);
        }
    }

    public URI endpointUri() {
        int localPort = tomcat.getConnector().getLocalPort();
        if (localPort <= 0) {
            throw new IllegalStateException("Function Catalog MCP server is not listening.");
        }
        return URI.create("http://127.0.0.1:" + localPort + endpoint);
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
            tomcat.stop();
            tomcat.destroy();
        } catch (Exception exception) {
            failure = appendFailure(failure, new IllegalStateException("Failed to stop embedded MCP HTTP server.", exception));
        }
        try {
            transportProvider.close();
        } catch (RuntimeException exception) {
            failure = appendFailure(failure, exception);
        }
        try {
            deleteRecursively(baseDirectory);
        } catch (RuntimeException exception) {
            failure = appendFailure(failure, exception);
        }

        if (failure != null) {
            throw failure;
        }
    }

    private static Path createBaseDirectory() {
        try {
            return Files.createTempDirectory("tavall-function-mcp-");
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to create Function Catalog MCP base directory.", exception);
        }
    }

    private static void closeFailedStart(
            McpSyncServer mcpServer,
            HttpServletStreamableServerTransportProvider transportProvider,
            Tomcat tomcat,
            Path baseDirectory
    ) {
        try {
            mcpServer.close();
        } catch (RuntimeException ignored) {
            // Preserve the startup failure.
        }
        try {
            tomcat.destroy();
        } catch (Exception ignored) {
            // Preserve the startup failure.
        }
        try {
            transportProvider.close();
        } catch (RuntimeException ignored) {
            // Preserve the startup failure.
        }
        try {
            deleteRecursively(baseDirectory);
        } catch (RuntimeException ignored) {
            // Preserve the startup failure.
        }
    }

    private static RuntimeException appendFailure(RuntimeException current, RuntimeException next) {
        if (current == null) {
            return next;
        }
        current.addSuppressed(next);
        return current;
    }

    private static void deleteRecursively(Path root) {
        if (root == null || !Files.exists(root)) {
            return;
        }
        try (var paths = Files.walk(root)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException exception) {
                    throw new UncheckedIOException(exception);
                }
            });
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to delete Function Catalog MCP base directory.", exception);
        }
    }
}

package org.tavall.ai.mcp.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures.SyncPromptSpecification;
import io.modelcontextprotocol.server.McpServerFeatures.SyncResourceSpecification;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider;
import jakarta.servlet.http.HttpServlet;
import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.tavall.ai.core.catalog.AIFunctionCatalog;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/** Reusable long-lived Streamable HTTP MCP host for a Function Catalog. */
public final class AIFunctionMcpStandaloneHttpServer implements AutoCloseable {
    private final Configuration configuration;
    private final Path baseDirectory;
    private final Tomcat tomcat;
    private final McpSyncServer mcpServer;
    private final HttpServletStreamableServerTransportProvider transportProvider;
    private final AtomicBoolean closed = new AtomicBoolean();

    private AIFunctionMcpStandaloneHttpServer(
            Configuration configuration,
            Path baseDirectory,
            Tomcat tomcat,
            McpSyncServer mcpServer,
            HttpServletStreamableServerTransportProvider transportProvider
    ) {
        this.configuration = Objects.requireNonNull(configuration, "configuration");
        this.baseDirectory = Objects.requireNonNull(baseDirectory, "baseDirectory");
        this.tomcat = Objects.requireNonNull(tomcat, "tomcat");
        this.mcpServer = Objects.requireNonNull(mcpServer, "mcpServer");
        this.transportProvider = Objects.requireNonNull(transportProvider, "transportProvider");
    }

    public static AIFunctionMcpStandaloneHttpServer start(
            AIFunctionCatalog catalog,
            Configuration configuration,
            List<SyncResourceSpecification> resources,
            List<SyncPromptSpecification> prompts,
            Map<String, AIFunctionMcpToolPublisher.ToolPresentation> presentations,
            List<ServletRegistration> supplementalServlets
    ) {
        AIFunctionCatalog safeCatalog = Objects.requireNonNull(catalog, "catalog");
        Configuration safeConfiguration = Objects.requireNonNull(configuration, "configuration");
        List<SyncResourceSpecification> safeResources = List.copyOf(Objects.requireNonNull(resources, "resources"));
        List<SyncPromptSpecification> safePrompts = List.copyOf(Objects.requireNonNull(prompts, "prompts"));
        Map<String, AIFunctionMcpToolPublisher.ToolPresentation> safePresentations = Map.copyOf(
                Objects.requireNonNull(presentations, "presentations")
        );
        List<ServletRegistration> safeSupplementalServlets = List.copyOf(
                Objects.requireNonNull(supplementalServlets, "supplementalServlets")
        );
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        McpJsonMapper jsonMapper = new JacksonMcpJsonMapper(objectMapper);
        AIFunctionMcpToolPublisher publisher = new AIFunctionMcpToolPublisher(objectMapper);
        HttpServletStreamableServerTransportProvider transportProvider =
                HttpServletStreamableServerTransportProvider.builder()
                        .jsonMapper(jsonMapper)
                        .mcpEndpoint(safeConfiguration.endpoint())
                        .build();
        McpSyncServer mcpServer = McpServer.sync(transportProvider)
                .serverInfo(safeConfiguration.serverName(), safeConfiguration.serverVersion())
                .instructions(safeConfiguration.instructions())
                .jsonMapper(jsonMapper)
                .tools(publisher.toolSpecifications(safeCatalog, safePresentations))
                .resources(safeResources)
                .prompts(safePrompts)
                .build();

        Path baseDirectory = createBaseDirectory();
        Tomcat tomcat = new Tomcat();
        tomcat.setBaseDir(baseDirectory.toString());
        tomcat.setPort(safeConfiguration.port());
        tomcat.getConnector();
        tomcat.getConnector().setProperty("address", safeConfiguration.address());
        Context context = tomcat.addContext(safeConfiguration.contextPath(), baseDirectory.toAbsolutePath().toString());
        Tomcat.addServlet(context, "tavallFunctionCatalogStandaloneMcp", transportProvider);
        context.addServletMappingDecoded(safeConfiguration.endpoint(), "tavallFunctionCatalogStandaloneMcp");
        context.addServletMappingDecoded(safeConfiguration.endpoint() + "/*", "tavallFunctionCatalogStandaloneMcp");
        registerSupplementalServlets(context, safeSupplementalServlets);
        try {
            tomcat.start();
            return new AIFunctionMcpStandaloneHttpServer(
                    safeConfiguration, baseDirectory, tomcat, mcpServer, transportProvider
            );
        } catch (Exception exception) {
            closeFailedStart(mcpServer, transportProvider, tomcat, baseDirectory);
            throw new IllegalStateException("Failed to start standalone Function Catalog MCP HTTP server.", exception);
        }
    }

    public static AIFunctionMcpStandaloneHttpServer start(
            AIFunctionCatalog catalog,
            Configuration configuration,
            List<SyncResourceSpecification> resources,
            List<SyncPromptSpecification> prompts,
            Map<String, AIFunctionMcpToolPublisher.ToolPresentation> presentations
    ) {
        return start(catalog, configuration, resources, prompts, presentations, List.of());
    }

    public static AIFunctionMcpStandaloneHttpServer start(
            AIFunctionCatalog catalog,
            Configuration configuration,
            List<SyncResourceSpecification> resources,
            List<SyncPromptSpecification> prompts
    ) {
        return start(catalog, configuration, resources, prompts, Map.of(), List.of());
    }

    public static AIFunctionMcpStandaloneHttpServer start(AIFunctionCatalog catalog, Configuration configuration) {
        return start(catalog, configuration, List.of(), List.of(), Map.of(), List.of());
    }

    public int port() {
        int localPort = tomcat.getConnector().getLocalPort();
        if (localPort <= 0) {
            throw new IllegalStateException("Standalone Function Catalog MCP server is not listening.");
        }
        return localPort;
    }

    public String endpointPath() {
        return configuration.contextPath() + configuration.endpoint();
    }

    public URI localEndpointUri() {
        return URI.create("http://127.0.0.1:" + port() + endpointPath());
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
            failure = appendFailure(failure, new IllegalStateException(
                    "Failed to stop standalone Function Catalog MCP HTTP server.", exception
            ));
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

    private static void registerSupplementalServlets(Context context, List<ServletRegistration> registrations) {
        Set<String> names = new HashSet<>();
        Set<String> mappings = new HashSet<>();
        for (ServletRegistration registration : registrations) {
            if (!names.add(registration.name())) {
                throw new IllegalArgumentException("Duplicate supplemental servlet name: " + registration.name());
            }
            Tomcat.addServlet(context, registration.name(), registration.servlet());
            for (String mapping : registration.mappings()) {
                if (!mappings.add(mapping)) {
                    throw new IllegalArgumentException("Duplicate supplemental servlet mapping: " + mapping);
                }
                context.addServletMappingDecoded(mapping, registration.name());
            }
        }
    }

    private static Path createBaseDirectory() {
        try {
            return Files.createTempDirectory("tavall-function-mcp-http-");
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to create standalone Function Catalog MCP base directory.", exception);
        }
    }

    private static void closeFailedStart(
            McpSyncServer mcpServer,
            HttpServletStreamableServerTransportProvider transportProvider,
            Tomcat tomcat,
            Path baseDirectory
    ) {
        try { mcpServer.close(); } catch (RuntimeException ignored) { }
        try { tomcat.destroy(); } catch (Exception ignored) { }
        try { transportProvider.close(); } catch (RuntimeException ignored) { }
        try { deleteRecursively(baseDirectory); } catch (RuntimeException ignored) { }
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
        try (java.util.stream.Stream<Path> paths = Files.walk(root)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException exception) {
                    throw new UncheckedIOException(exception);
                }
            });
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to delete standalone Function Catalog MCP base directory.", exception);
        }
    }

    public record ServletRegistration(String name, HttpServlet servlet, List<String> mappings) {
        public ServletRegistration {
            name = requireText(name, "name");
            servlet = Objects.requireNonNull(servlet, "servlet");
            mappings = List.copyOf(Objects.requireNonNull(mappings, "mappings"));
            if (mappings.isEmpty()) {
                throw new IllegalArgumentException("Servlet mappings must not be empty");
            }
            for (String mapping : mappings) {
                if (mapping == null || mapping.isBlank() || !mapping.startsWith("/")) {
                    throw new IllegalArgumentException("Servlet mappings must be absolute paths");
                }
            }
        }
    }

    public record Configuration(
            String address,
            int port,
            String contextPath,
            String endpoint,
            String serverName,
            String serverVersion,
            String instructions
    ) {
        public Configuration {
            address = requireText(address, "address");
            if (port < 0 || port > 65_535) {
                throw new IllegalArgumentException("port must be between 0 and 65535");
            }
            contextPath = normalizeContextPath(contextPath);
            endpoint = normalizeEndpoint(endpoint);
            serverName = requireText(serverName, "serverName");
            serverVersion = requireText(serverVersion, "serverVersion");
            instructions = Objects.requireNonNullElse(instructions, "");
        }

        public static Configuration defaults(String serverName, String serverVersion, String instructions) {
            return new Configuration("127.0.0.1", 9000, "", "/mcp", serverName, serverVersion, instructions);
        }

        private static String normalizeContextPath(String value) {
            if (value == null || value.isBlank() || "/".equals(value)) {
                return "";
            }
            String normalized = value.startsWith("/") ? value : "/" + value;
            return normalized.endsWith("/") ? normalized.substring(0, normalized.length() - 1) : normalized;
        }

        private static String normalizeEndpoint(String value) {
            if (value == null || value.isBlank() || "/".equals(value)) {
                return "/mcp";
            }
            String normalized = value.startsWith("/") ? value : "/" + value;
            return normalized.endsWith("/") ? normalized.substring(0, normalized.length() - 1) : normalized;
        }

        private static String requireText(String value, String fieldName) {
            if (value != null && !value.isBlank()) {
                return value;
            }
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }
}

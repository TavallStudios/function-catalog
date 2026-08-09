package org.tavall.ai.mcp.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import org.tavall.ai.core.catalog.AIFunctionCatalog;
import org.tavall.ai.core.catalog.AIFunctionRegistrar;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public final class AIFunctionMcpServerLauncher {
    public static void main(String[] args) {
        LaunchConfiguration configuration = LaunchConfiguration.parse(args);
        if (configuration.help()) {
            System.out.println(LaunchConfiguration.usage());
            return;
        }

        try {
            serve(configuration);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to start FunctionCatalog MCP server.", exception);
        }
    }

    public static void serve(LaunchConfiguration configuration) throws Exception {
        LaunchConfiguration safeConfiguration = requireValue(configuration, "configuration");
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        AIFunctionCatalog catalog = new AIFunctionCatalog(objectMapper);
        if (safeConfiguration.stateFile() != null || safeConfiguration.snapshotFile() != null) {
            catalog.configureStateFiles(safeConfiguration.stateFile(), safeConfiguration.snapshotFile());
        }

        for (String registrarClassName : safeConfiguration.registrarClasses()) {
            instantiateRegistrar(registrarClassName).register(catalog);
        }
        if (!safeConfiguration.scanPackages().isEmpty()) {
            catalog.scanPackages(safeConfiguration.scanPackages());
        }
        if (catalog.getFunctionDefinitions().isEmpty()) {
            throw new IllegalStateException("No @AIFunction methods were registered.");
        }

        JacksonMcpJsonMapper jsonMapper = new JacksonMcpJsonMapper(objectMapper);
        StdioServerTransportProvider transportProvider = new StdioServerTransportProvider(jsonMapper);
        McpSyncServer server = McpServer.sync(transportProvider)
                .serverInfo("FunctionCatalog MCP", "1.0.0")
                .instructions("Call the cataloged @AIFunction methods. Disabled functions return structured errors.")
                .jsonMapper(jsonMapper)
                .tools(new AIFunctionMcpToolPublisher(objectMapper).toolSpecifications(catalog))
                .build();

        CountDownLatch latch = new CountDownLatch(1);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                server.close();
            } catch (Exception ignored) {
                // ignore shutdown close failures
            }
            transportProvider.close();
            latch.countDown();
        }, "function-catalog-mcp-shutdown"));
        latch.await();
    }

    private static AIFunctionRegistrar instantiateRegistrar(String registrarClassName) {
        String safeRegistrarClassName = requireText(registrarClassName, "registrarClassName");
        try {
            Class<?> registrarClass = Class.forName(safeRegistrarClassName);
            Object instance = registrarClass.getDeclaredConstructor().newInstance();
            if (!(instance instanceof AIFunctionRegistrar registrar)) {
                throw new IllegalStateException("Registrar class does not implement AIFunctionRegistrar: " + safeRegistrarClassName);
            }
            return registrar;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to instantiate registrar class: " + safeRegistrarClassName, exception);
        }
    }

    private static <T> T requireValue(T value, String fieldName) {
        if (value != null) {
            return value;
        }
        throw new IllegalArgumentException(fieldName + " must not be null");
    }

    private static String requireText(String value, String fieldName) {
        if (value != null && !value.isBlank()) {
            return value;
        }
        throw new IllegalArgumentException(fieldName + " must not be blank");
    }

    public record LaunchConfiguration(
            List<String> registrarClasses,
            List<String> scanPackages,
            Path stateFile,
            Path snapshotFile,
            boolean help
    ) {
        public static LaunchConfiguration parse(String[] args) {
            List<String> registrarClasses = new ArrayList<>();
            List<String> scanPackages = new ArrayList<>();
            Path stateFile = null;
            Path snapshotFile = null;
            boolean help = false;

            for (String arg : requireValue(args, "args")) {
                if (arg == null || arg.isBlank()) {
                    continue;
                }
                if ("--help".equals(arg) || "-h".equals(arg)) {
                    help = true;
                    continue;
                }
                if (arg.startsWith("--registrar-class=")) {
                    registrarClasses.add(arg.substring("--registrar-class=".length()));
                    continue;
                }
                if (arg.startsWith("--scan=")) {
                    scanPackages.add(arg.substring("--scan=".length()));
                    continue;
                }
                if (arg.startsWith("--state-file=")) {
                    stateFile = Path.of(arg.substring("--state-file=".length())).toAbsolutePath().normalize();
                    continue;
                }
                if (arg.startsWith("--snapshot-file=")) {
                    snapshotFile = Path.of(arg.substring("--snapshot-file=".length())).toAbsolutePath().normalize();
                    continue;
                }
                throw new IllegalArgumentException("Unknown argument: " + arg);
            }

            if (!help && registrarClasses.isEmpty() && scanPackages.isEmpty()) {
                throw new IllegalArgumentException("Provide at least one --registrar-class or --scan argument.");
            }

            return new LaunchConfiguration(List.copyOf(registrarClasses), List.copyOf(scanPackages), stateFile, snapshotFile, help);
        }

        public static String usage() {
            return String.join(System.lineSeparator(),
                    "Usage: java ... org.tavall.ai.mcp.server.AIFunctionMcpServerLauncher [options]",
                    "  --registrar-class=<fqcn>   Register instances from an AIFunctionRegistrar (repeatable)",
                    "  --scan=<package>           Fallback package scan for @AIFunction methods (repeatable)",
                    "  --state-file=<path>        Persisted function state JSON path",
                    "  --snapshot-file=<path>     Snapshot JSON path for live catalog visibility",
                    "  --help                     Show this help"
            );
        }
    }
}

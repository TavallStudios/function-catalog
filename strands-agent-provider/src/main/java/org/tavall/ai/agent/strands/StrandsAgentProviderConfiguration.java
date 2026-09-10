package org.tavall.ai.agent.strands;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Immutable process and model configuration for the standalone Strands MCP runtime. */
public record StrandsAgentProviderConfiguration(
        String command,
        List<String> arguments,
        Map<String, String> environment,
        Duration initializationTimeout,
        Duration requestTimeout,
        String modelId
) {
    public StrandsAgentProviderConfiguration {
        command = requireExecutable(command);
        arguments = List.copyOf(arguments == null ? List.of() : new ArrayList<>(arguments));
        environment = Map.copyOf(environment == null ? Map.of() : new LinkedHashMap<>(environment));
        initializationTimeout = requirePositive(initializationTimeout, "initializationTimeout");
        requestTimeout = requirePositive(requestTimeout, "requestTimeout");
        modelId = modelId == null ? "" : modelId.trim();
    }

    public static StrandsAgentProviderConfiguration node(
            Path nodeExecutable,
            Path bridgeEntrypoint,
            Map<String, String> environment,
            Duration requestTimeout,
            String modelId
    ) {
        Path safeNodeExecutable = requireExecutablePath(nodeExecutable, "nodeExecutable");
        Path safeBridgeEntrypoint = Objects.requireNonNull(bridgeEntrypoint, "bridgeEntrypoint")
                .toAbsolutePath()
                .normalize();
        if (!Files.isRegularFile(safeBridgeEntrypoint)) {
            throw new IllegalArgumentException("Strands bridge entrypoint is unavailable: " + safeBridgeEntrypoint);
        }
        return new StrandsAgentProviderConfiguration(
                safeNodeExecutable.toString(),
                List.of(safeBridgeEntrypoint.toString()),
                environment,
                Duration.ofSeconds(20),
                requestTimeout,
                modelId
        );
    }

    public static StrandsAgentProviderConfiguration executable(
            Path executable,
            List<String> arguments,
            Map<String, String> environment,
            Duration requestTimeout,
            String modelId
    ) {
        Path safeExecutable = requireExecutablePath(executable, "executable");
        return new StrandsAgentProviderConfiguration(
                safeExecutable.toString(),
                arguments,
                environment,
                Duration.ofSeconds(20),
                requestTimeout,
                modelId
        );
    }

    private static String requireExecutable(String value) {
        String safeValue = requireText(value, "command");
        Path path = Path.of(safeValue);
        if (!path.isAbsolute()) {
            throw new IllegalArgumentException("command must be an absolute executable path");
        }
        return requireExecutablePath(path, "command").toString();
    }

    private static Path requireExecutablePath(Path value, String fieldName) {
        Path safePath = Objects.requireNonNull(value, fieldName).toAbsolutePath().normalize();
        if (!Files.isRegularFile(safePath) || !Files.isExecutable(safePath)) {
            throw new IllegalArgumentException(fieldName + " is unavailable or not executable: " + safePath);
        }
        return safePath;
    }

    private static Duration requirePositive(Duration value, String fieldName) {
        Duration safeValue = Objects.requireNonNull(value, fieldName);
        if (safeValue.isZero() || safeValue.isNegative()) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return safeValue;
    }

    private static String requireText(String value, String fieldName) {
        if (value != null && !value.isBlank()) {
            return value.trim();
        }
        throw new IllegalArgumentException(fieldName + " must not be blank");
    }
}

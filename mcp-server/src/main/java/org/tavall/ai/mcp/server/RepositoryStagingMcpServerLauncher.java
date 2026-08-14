package org.tavall.ai.mcp.server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Explicit MCP launch profile for the Tavall repository-staging function set.
 *
 * <p>The general Function Catalog launcher intentionally does not auto-discover registrars. This
 * profile opts into {@code RepositoryStagingRegistrar} by class name so staging tools are exposed
 * only when an operator/runtime chooses this launcher. The registrar itself fails closed unless an
 * explicit GitHub token and repository allowlist are configured.</p>
 */
public final class RepositoryStagingMcpServerLauncher {
    static final String REGISTRAR_ARGUMENT =
            "--registrar-class=org.tavall.ai.staging.RepositoryStagingRegistrar";

    private RepositoryStagingMcpServerLauncher() {
    }

    public static void main(String[] args) {
        AIFunctionMcpServerLauncher.main(arguments(args).toArray(String[]::new));
    }

    static List<String> arguments(String[] args) {
        Objects.requireNonNull(args, "args");
        ArrayList<String> result = new ArrayList<>(args.length + 1);
        result.add(REGISTRAR_ARGUMENT);
        result.addAll(Arrays.asList(args));
        return List.copyOf(result);
    }
}

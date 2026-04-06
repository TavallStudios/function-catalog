package org.tavall.ai.core.catalog;

import org.tavall.ai.core.annotation.AIFunction;

public interface InterfaceAnnotatedFunction {
    @AIFunction(name = "interface_echo", description = "Echoes a message from an interface annotation")
    String echo(String message);
}

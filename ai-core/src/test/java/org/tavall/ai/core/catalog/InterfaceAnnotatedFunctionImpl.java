package org.tavall.ai.core.catalog;

public final class InterfaceAnnotatedFunctionImpl implements InterfaceAnnotatedFunction {
    @Override
    public String echo(String message) {
        return "echo:" + message;
    }
}

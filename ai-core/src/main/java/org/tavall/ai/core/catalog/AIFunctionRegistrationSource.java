package org.tavall.ai.core.catalog;

public enum AIFunctionRegistrationSource {
    MANUAL("manual"),
    SCAN("scan");

    private final String wireValue;

    AIFunctionRegistrationSource(String wireValue) {
        this.wireValue = wireValue;
    }

    public String getWireValue() {
        return wireValue;
    }
}

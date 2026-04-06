/*
 * TJVD License (TJ Valentine’s Discretionary License) — Version 1.0 (2025)
 *
 * Copyright (c) 2025 Taheesh Valentine
 *
 * This source code is protected under the TJVD License.
 * SEE LICENSE.TXT
 */

package org.tavall.gemini.enums;

public enum GeminiAPIVersion {

    V1("v1"),
    V1_BETA("v1beta");
    private final String version;
    GeminiAPIVersion(String version) {

        this.version = version;
    }

    @Override
    public String toString() {
        return version;
    }

}
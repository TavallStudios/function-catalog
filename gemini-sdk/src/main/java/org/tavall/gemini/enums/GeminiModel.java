/*
 * TJVD License (TJ Valentine’s Discretionary License) — Version 1.0 (2025)
 *
 * Copyright (c) 2025 Taheesh Valentine
 *
 * This source code is protected under the TJVD License.
 * SEE LICENSE.TXT
 */

package org.tavall.gemini.enums;

public enum GeminiModel {

    GEMINI_3_FLASH("gemini-3-flash-preview"),
    GEMINI_3_PRO("gemini-3-pro-preview"),
    GEMINI_3_PRO_IMAGE_PREVIEW("gemini-3-pro-image-preview"),
    GEMINI_2_5_FLASH("gemini-2.5-flash"),
    GEMINI_2_FLASH_LITE("gemini-2-flash-lite");

    private final String model;

    GeminiModel(String model) {
        this.model = model;
    }


    @Override
    public String toString() {
        return model;
    }
}

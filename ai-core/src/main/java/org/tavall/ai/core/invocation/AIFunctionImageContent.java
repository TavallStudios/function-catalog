package org.tavall.ai.core.invocation;

import java.util.Arrays;

public final class AIFunctionImageContent {
    private final byte[] data;
    private final String mimeType;

    public AIFunctionImageContent(byte[] data, String mimeType) {
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("data must not be empty");
        }
        if (mimeType == null || mimeType.isBlank() || !mimeType.startsWith("image/")) {
            throw new IllegalArgumentException("mimeType must be an image MIME type");
        }
        this.data = Arrays.copyOf(data, data.length);
        this.mimeType = mimeType.strip().toLowerCase();
    }

    public byte[] getData() {
        return Arrays.copyOf(data, data.length);
    }

    public String getMimeType() {
        return mimeType;
    }
}

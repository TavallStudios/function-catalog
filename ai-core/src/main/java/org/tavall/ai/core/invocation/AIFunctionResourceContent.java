package org.tavall.ai.core.invocation;

import java.net.URI;
import java.util.Arrays;

/** Binary resource content with a logical URI and MIME type. */
public final class AIFunctionResourceContent implements AIFunctionContent {
    private final byte[] data;
    private final String mimeType;
    private final String uri;

    public AIFunctionResourceContent(byte[] data, String mimeType, String uri) {
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("data must not be empty");
        }
        if (mimeType == null || mimeType.isBlank() || !mimeType.contains("/")) {
            throw new IllegalArgumentException("mimeType must be a valid MIME type");
        }
        if (uri == null || uri.isBlank()) {
            throw new IllegalArgumentException("uri must not be blank");
        }
        URI parsed = URI.create(uri.strip());
        if (!parsed.isAbsolute()) {
            throw new IllegalArgumentException("uri must be absolute");
        }
        this.data = Arrays.copyOf(data, data.length);
        this.mimeType = mimeType.strip().toLowerCase();
        this.uri = parsed.toASCIIString();
    }

    @Override
    public String getType() {
        return "resource";
    }

    @Override
    public byte[] getData() {
        return Arrays.copyOf(data, data.length);
    }

    @Override
    public String getMimeType() {
        return mimeType;
    }

    public String getUri() {
        return uri;
    }
}

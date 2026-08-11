package org.tavall.ai.core.invocation;

/**
 * Provider-neutral rich content returned alongside a function's structured payload.
 */
public sealed interface AIFunctionContent permits AIFunctionImageContent, AIFunctionResourceContent {
    String getType();

    byte[] getData();

    String getMimeType();
}

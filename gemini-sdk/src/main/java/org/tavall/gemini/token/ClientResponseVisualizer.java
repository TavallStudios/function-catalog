package org.tavall.gemini.token;

import org.tavall.gemini.clients.response.enums.ResponseStatus;
import org.tavall.gemini.clients.response.metadata.ClientResponseMetadata;
import org.tavall.gemini.enums.GeminiModel;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Map;

public final class ClientResponseVisualizer {

    private ClientResponseVisualizer() {
    }

    public static ClientResponseMetadata buildMetadata(Object response,
                                                       GeminiModel model,
                                                       Duration latency,
                                                       ResponseStatus status) {
        int totalTokens = extractTokenCount(response);
        Duration resolvedLatency = latency != null ? latency : Duration.ZERO;
        ResponseStatus resolvedStatus = status != null ? status : ResponseStatus.COMPLETED;
        return new ClientResponseMetadata(model, totalTokens, resolvedLatency, resolvedStatus);
    }

    public static String format(ClientResponseMetadata metadata) {
        if (metadata == null) {
            return "Gemini response metadata unavailable.";
        }
        String tokens = metadata.totalTokenCount() >= 0
                ? String.valueOf(metadata.totalTokenCount())
                : "unknown";
        long ms = metadata.latency() != null ? metadata.latency().toMillis() : -1;
        String latency = ms >= 0 ? ms + "ms" : "unknown";
        return "model=" + metadata.modelUsed()
                + " tokens=" + tokens
                + " latency=" + latency
                + " status=" + metadata.responseStatus();
    }

    public static int extractTokenCount(Object response) {
        if (response == null) {
            return -1;
        }
        Object usage = invokeNoArg(response, "usageMetadata");
        if (usage == null) {
            usage = invokeNoArg(response, "getUsageMetadata");
        }
        if (usage == null) {
            return -1;
        }
        if (usage instanceof Map<?, ?> map) {
            Object value = map.get("totalTokenCount");
            if (value instanceof Number num) {
                return num.intValue();
            }
            Object fallback = map.get("totalTokens");
            if (fallback instanceof Number num) {
                return num.intValue();
            }
            return -1;
        }
        Integer total = readInt(usage, "totalTokenCount", "getTotalTokenCount", "totalTokens", "getTotalTokens");
        return total != null ? total : -1;
    }

    private static Integer readInt(Object target, String... methods) {
        for (String method : methods) {
            Object result = invokeNoArg(target, method);
            if (result instanceof Number num) {
                return num.intValue();
            }
        }
        return null;
    }

    private static Object invokeNoArg(Object target, String method) {
        try {
            Method m = target.getClass().getMethod(method);
            return m.invoke(target);
        } catch (Exception ignored) {
            return null;
        }
    }
}

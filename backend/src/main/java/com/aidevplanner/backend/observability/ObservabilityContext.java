package com.aidevplanner.backend.observability;

import org.slf4j.MDC;

public final class ObservabilityContext {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String REQUEST_ID_KEY = "requestId";

    private static final ThreadLocal<String> REQUEST_ID = new ThreadLocal<>();

    private ObservabilityContext() {
    }

    public static void setRequestId(String requestId) {
        if (requestId == null || requestId.isBlank()) {
            clear();
            return;
        }
        String normalized = requestId.trim();
        REQUEST_ID.set(normalized);
        MDC.put(REQUEST_ID_KEY, normalized);
    }

    public static String getRequestId() {
        return REQUEST_ID.get();
    }

    public static void clear() {
        REQUEST_ID.remove();
        MDC.remove(REQUEST_ID_KEY);
    }
}

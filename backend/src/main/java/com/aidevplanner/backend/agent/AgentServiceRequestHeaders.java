package com.aidevplanner.backend.agent;

import com.aidevplanner.backend.observability.ObservabilityContext;
import org.springframework.http.HttpHeaders;

public final class AgentServiceRequestHeaders {

    private AgentServiceRequestHeaders() {
    }

    public static void addTraceHeaders(HttpHeaders headers) {
        String requestId = ObservabilityContext.getRequestId();
        if (requestId != null && !requestId.isBlank()) {
            headers.set(ObservabilityContext.REQUEST_ID_HEADER, requestId);
        }
    }
}

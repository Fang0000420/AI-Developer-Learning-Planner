package com.aidevplanner.backend.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String requestId = firstPresent(request.getHeader(ObservabilityContext.REQUEST_ID_HEADER), UUID.randomUUID().toString());
        long startedAt = System.nanoTime();
        ObservabilityContext.setRequestId(requestId);
        response.setHeader(ObservabilityContext.REQUEST_ID_HEADER, requestId);

        try {
            filterChain.doFilter(request, response);
        } catch (Exception exception) {
            LOGGER.error(
                    "request failed method={} path={} status={} latencyMs={} requestId={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    elapsedMs(startedAt),
                    requestId,
                    exception
            );
            throw exception;
        } finally {
            long latencyMs = elapsedMs(startedAt);
            int status = response.getStatus();
            if (status >= 500) {
                LOGGER.error(
                        "request completed method={} path={} status={} latencyMs={} requestId={}",
                        request.getMethod(),
                        request.getRequestURI(),
                        status,
                        latencyMs,
                        requestId
                );
            } else if (status >= 400) {
                LOGGER.warn(
                        "request completed method={} path={} status={} latencyMs={} requestId={}",
                        request.getMethod(),
                        request.getRequestURI(),
                        status,
                        latencyMs,
                        requestId
                );
            } else {
                LOGGER.info(
                        "request completed method={} path={} status={} latencyMs={} requestId={}",
                        request.getMethod(),
                        request.getRequestURI(),
                        status,
                        latencyMs,
                        requestId
                );
            }
            ObservabilityContext.clear();
        }
    }

    private String firstPresent(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private long elapsedMs(long startedAt) {
        return Math.max(1, (System.nanoTime() - startedAt) / 1_000_000);
    }
}

package com.aidevplanner.backend.common;

import java.time.Instant;
import java.util.Map;

public record ApiErrorResponse(
        String status,
        String message,
        Map<String, String> errors,
        Instant timestamp
) {

    public static ApiErrorResponse of(String status, String message) {
        return new ApiErrorResponse(status, message, Map.of(), Instant.now());
    }

    public static ApiErrorResponse of(String status, String message, Map<String, String> errors) {
        return new ApiErrorResponse(status, message, errors, Instant.now());
    }
}

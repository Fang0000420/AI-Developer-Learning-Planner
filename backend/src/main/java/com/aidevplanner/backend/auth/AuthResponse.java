package com.aidevplanner.backend.auth;

public record AuthResponse(
        String token,
        String tokenType,
        Long userId,
        String username
) {
}

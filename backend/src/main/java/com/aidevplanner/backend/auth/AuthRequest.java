package com.aidevplanner.backend.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AuthRequest(
        @NotBlank(message = "Username is required.")
        @Size(max = 100, message = "Username cannot exceed 100 characters.")
        String username,

        @NotBlank(message = "Password is required.")
        @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters.")
        String password
) {
}

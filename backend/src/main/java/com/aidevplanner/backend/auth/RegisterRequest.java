package com.aidevplanner.backend.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Username is required.")
        @Size(min = 3, max = 100, message = "Username must be between 3 and 100 characters.")
        String username,

        @Email(message = "Email must be valid.")
        @Size(max = 255, message = "Email cannot exceed 255 characters.")
        String email,

        @NotBlank(message = "Password is required.")
        @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters.")
        String password
) {
}

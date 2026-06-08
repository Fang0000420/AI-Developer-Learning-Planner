package com.aidevplanner.backend.goal;

import com.aidevplanner.backend.common.validation.AllowedGoalDurationDays;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record GoalCreateRequest(
        @Positive(message = "User id must be positive.")
        Long userId,

        @Size(max = 3000, message = "Technical background cannot exceed 3000 characters.")
        String technicalBackground,

        @NotBlank(message = "Title is required.")
        @Size(max = 255, message = "Title cannot exceed 255 characters.")
        String title,

        @Size(max = 5000, message = "Description cannot exceed 5000 characters.")
        String description,

        @NotNull(message = "Duration days is required.")
        @AllowedGoalDurationDays
        Integer durationDays,

        @NotNull(message = "Daily available hours is required.")
        @DecimalMin(value = "0.5", message = "Daily available hours must be at least 0.5.")
        @DecimalMax(value = "12.0", message = "Daily available hours cannot exceed 12.")
        @Digits(integer = 2, fraction = 1, message = "Daily available hours must use at most one decimal place.")
        BigDecimal dailyAvailableHours
) {
}

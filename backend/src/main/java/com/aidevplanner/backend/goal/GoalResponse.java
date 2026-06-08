package com.aidevplanner.backend.goal;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record GoalResponse(
        Long id,
        Long userId,
        String title,
        String description,
        Integer durationDays,
        GoalStatus status,
        BigDecimal dailyAvailableHours,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static GoalResponse from(Goal goal) {
        return new GoalResponse(
                goal.getId(),
                goal.getUser().getId(),
                goal.getTitle(),
                goal.getDescription(),
                goal.getDurationDays(),
                goal.getStatus(),
                goal.getUser().getDailyAvailableHours(),
                goal.getCreatedAt(),
                goal.getUpdatedAt()
        );
    }
}

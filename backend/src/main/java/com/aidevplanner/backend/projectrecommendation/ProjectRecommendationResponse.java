package com.aidevplanner.backend.projectrecommendation;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ProjectRecommendationResponse(
        Long runId,
        Long goalId,
        String recommendedProject,
        String reason,
        String difficulty,
        Integer durationDays,
        BigDecimal dailyTimeHours,
        List<String> coreTechStack,
        List<String> finalDeliverables,
        LocalDateTime createdAt
) {
}

package com.aidevplanner.backend.projectrecommendation;

import java.math.BigDecimal;
import java.util.List;

public record ProjectRecommendResponse(
        String recommendedProject,
        String reason,
        String difficulty,
        Integer durationDays,
        BigDecimal dailyTimeHours,
        List<String> coreTechStack,
        List<String> finalDeliverables
) {
}

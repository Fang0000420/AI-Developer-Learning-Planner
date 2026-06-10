package com.aidevplanner.backend.profile;

import java.math.BigDecimal;

public record ProfileAnalyzeRequest(
        String background,
        String goal,
        BigDecimal dailyAvailableHours,
        String responseLanguage
) {
}

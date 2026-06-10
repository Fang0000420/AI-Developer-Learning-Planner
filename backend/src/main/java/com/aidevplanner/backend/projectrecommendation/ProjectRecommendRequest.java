package com.aidevplanner.backend.projectrecommendation;

import com.aidevplanner.backend.goaldecomposition.SubGoalResponse;
import com.aidevplanner.backend.skillgap.SkillGapResponse;

import java.math.BigDecimal;
import java.util.List;

public record ProjectRecommendRequest(
        String mainGoal,
        List<String> currentSkills,
        List<String> strengths,
        List<String> weaknesses,
        List<SubGoalResponse> subGoals,
        List<SkillGapResponse> skillGaps,
        Integer durationDays,
        BigDecimal dailyAvailableHours,
        String responseLanguage
) {
}

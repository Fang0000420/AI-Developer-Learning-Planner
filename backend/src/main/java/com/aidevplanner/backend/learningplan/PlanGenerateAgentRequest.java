package com.aidevplanner.backend.learningplan;

import com.aidevplanner.backend.goaldecomposition.SubGoalResponse;
import com.aidevplanner.backend.skillgap.SkillGapResponse;

import java.math.BigDecimal;
import java.util.List;

public record PlanGenerateAgentRequest(
        String mainGoal,
        List<String> currentSkills,
        List<String> strengths,
        List<String> weaknesses,
        List<SubGoalResponse> subGoals,
        List<SkillGapResponse> skillGaps,
        String recommendedProject,
        String projectReason,
        String difficulty,
        List<String> coreTechStack,
        List<String> finalDeliverables,
        Integer durationDays,
        BigDecimal dailyAvailableHours,
        String responseLanguage
) {
}

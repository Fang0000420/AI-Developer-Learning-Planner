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
        String userProfileSummary,
        String pacePreference,
        String timeBudgetNote,
        String manualCorrection,
        List<String> profileEvidence,
        List<SubGoalResponse> subGoals,
        List<SkillGapResponse> skillGaps,
        String recommendedProject,
        String projectReason,
        String difficulty,
        List<String> coreTechStack,
        List<String> finalDeliverables,
        List<String> planningConstraints,
        List<String> recentFeedback,
        String knowledgeContext,
        Integer durationDays,
        BigDecimal dailyAvailableHours,
        String responseLanguage
) {
}

package com.aidevplanner.backend.skillgap;

import com.aidevplanner.backend.goaldecomposition.SubGoalResponse;

import java.util.List;

public record SkillGapAnalyzeRequest(
        String mainGoal,
        List<String> currentSkills,
        List<String> strengths,
        List<String> weaknesses,
        List<SubGoalResponse> subGoals
) {
}

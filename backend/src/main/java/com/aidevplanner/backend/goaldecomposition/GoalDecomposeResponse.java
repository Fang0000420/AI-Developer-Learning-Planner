package com.aidevplanner.backend.goaldecomposition;

import java.util.List;

public record GoalDecomposeResponse(
        List<SubGoalResponse> subGoals
) {
}

package com.aidevplanner.backend.goaldecomposition;

import java.time.LocalDateTime;
import java.util.List;

public record GoalDecompositionResponse(
        Long runId,
        Long goalId,
        List<SubGoalResponse> subGoals,
        LocalDateTime createdAt
) {
}

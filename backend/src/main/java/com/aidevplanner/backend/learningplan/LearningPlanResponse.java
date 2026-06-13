package com.aidevplanner.backend.learningplan;

import com.aidevplanner.backend.knowledge.KnowledgeBasisResponse;

import java.time.LocalDateTime;
import java.util.List;

public record LearningPlanResponse(
        Long id,
        Long goalId,
        Long userId,
        Long sourceAgentRunId,
        String planTitle,
        Integer durationDays,
        LearningPlanStatus status,
        List<PlanDayResponse> days,
        List<LearningPlanVersionSummaryResponse> versions,
        KnowledgeBasisResponse knowledgeBasis,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

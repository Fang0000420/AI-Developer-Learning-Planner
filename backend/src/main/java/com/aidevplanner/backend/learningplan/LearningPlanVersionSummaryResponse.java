package com.aidevplanner.backend.learningplan;

import java.time.LocalDateTime;
import java.util.List;

public record LearningPlanVersionSummaryResponse(
        Integer version,
        String trigger,
        String reason,
        Integer dayCount,
        Integer taskCount,
        Integer totalEstimatedMinutes,
        Integer minuteDelta,
        Integer taskDelta,
        List<Integer> affectedDayIndexes,
        LearningPlanVersionDiffResponse diff,
        boolean current,
        LocalDateTime createdAt
) {
}

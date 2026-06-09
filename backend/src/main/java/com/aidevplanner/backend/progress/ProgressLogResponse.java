package com.aidevplanner.backend.progress;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record ProgressLogResponse(
        Long id,
        Long planId,
        Long goalId,
        Long userId,
        Integer dayIndex,
        String userFeedback,
        List<Long> completedTaskIds,
        List<Long> unfinishedTaskIds,
        List<String> blockers,
        Map<String, Object> reviewResultJson,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

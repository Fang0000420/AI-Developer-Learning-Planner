package com.aidevplanner.backend.learningplan;

import java.util.List;

public record LearningPlanVersionDiffResponse(
        Integer addedTaskCount,
        Integer removedTaskCount,
        Integer updatedTaskCount,
        List<Integer> changedDayIndexes,
        List<LearningPlanVersionTaskChangeResponse> taskChanges
) {
}

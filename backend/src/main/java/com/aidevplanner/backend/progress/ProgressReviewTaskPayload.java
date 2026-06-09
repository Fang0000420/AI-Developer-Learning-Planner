package com.aidevplanner.backend.progress;

public record ProgressReviewTaskPayload(
        Long id,
        String title,
        String description,
        Integer estimatedMinutes,
        String type,
        String deliverable,
        String priority
) {
}

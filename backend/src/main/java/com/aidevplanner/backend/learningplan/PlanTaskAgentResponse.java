package com.aidevplanner.backend.learningplan;

public record PlanTaskAgentResponse(
        String title,
        String description,
        Integer estimatedMinutes,
        String type,
        String deliverable,
        String priority
) {
}

package com.aidevplanner.backend.skillgap;

public record SkillGapResponse(
        String skill,
        String currentLevel,
        String targetLevel,
        String priority,
        String reason
) {
}

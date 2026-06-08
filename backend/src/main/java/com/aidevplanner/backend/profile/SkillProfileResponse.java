package com.aidevplanner.backend.profile;

import java.time.LocalDateTime;
import java.util.List;

public record SkillProfileResponse(
        Long id,
        Long userId,
        Long goalId,
        List<String> currentSkills,
        List<String> strengths,
        List<String> weaknesses,
        String recommendedDirection,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static SkillProfileResponse from(SkillProfile profile) {
        return new SkillProfileResponse(
                profile.getId(),
                profile.getUser().getId(),
                profile.getGoal().getId(),
                profile.getCurrentSkills(),
                profile.getStrengths(),
                profile.getWeaknesses(),
                profile.getRecommendedDirection(),
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }
}

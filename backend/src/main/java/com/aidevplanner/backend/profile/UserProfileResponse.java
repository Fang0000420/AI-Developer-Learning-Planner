package com.aidevplanner.backend.profile;

import java.time.LocalDateTime;
import java.util.List;

public record UserProfileResponse(
        Long id,
        Long userId,
        Integer currentVersion,
        String profileSummary,
        String preferredLearningStyle,
        String pacePreference,
        String timeBudgetNote,
        String manualCorrection,
        List<String> currentSkills,
        List<String> strengths,
        List<String> weaknesses,
        List<String> focusAreas,
        List<String> riskSignals,
        List<String> evidence,
        String recommendedDirection,
        List<GoalProfileSnapshotResponse> recentSnapshots,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static UserProfileResponse from(UserProfile profile, List<GoalProfileSnapshotResponse> recentSnapshots) {
        return new UserProfileResponse(
                profile.getId(),
                profile.getUser().getId(),
                profile.getCurrentVersion(),
                profile.getProfileSummary(),
                profile.getPreferredLearningStyle(),
                profile.getPacePreference(),
                profile.getTimeBudgetNote(),
                profile.getManualCorrection(),
                profile.getCurrentSkills(),
                profile.getStrengths(),
                profile.getWeaknesses(),
                profile.getFocusAreas(),
                profile.getRiskSignals(),
                profile.getEvidence(),
                profile.getRecommendedDirection(),
                recentSnapshots,
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }
}

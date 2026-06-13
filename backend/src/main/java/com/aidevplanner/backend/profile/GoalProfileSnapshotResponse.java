package com.aidevplanner.backend.profile;

import java.time.LocalDateTime;
import java.util.List;

public record GoalProfileSnapshotResponse(
        Long id,
        Long goalId,
        String goalTitle,
        Long userProfileVersionId,
        Integer version,
        String summary,
        String preferredLearningStyle,
        String pacePreference,
        String timeBudgetNote,
        List<String> currentSkills,
        List<String> strengths,
        List<String> weaknesses,
        List<String> focusAreas,
        List<String> riskSignals,
        List<String> evidence,
        String recommendedDirection,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static GoalProfileSnapshotResponse from(GoalProfileSnapshot snapshot) {
        return new GoalProfileSnapshotResponse(
                snapshot.getId(),
                snapshot.getGoal().getId(),
                snapshot.getGoal().getTitle(),
                snapshot.getUserProfileVersion().getId(),
                snapshot.getUserProfileVersion().getVersion(),
                snapshot.getSnapshotSummary(),
                snapshot.getPreferredLearningStyle(),
                snapshot.getPacePreference(),
                snapshot.getTimeBudgetNote(),
                snapshot.getCurrentSkills(),
                snapshot.getStrengths(),
                snapshot.getWeaknesses(),
                snapshot.getFocusAreas(),
                snapshot.getRiskSignals(),
                snapshot.getEvidence(),
                snapshot.getRecommendedDirection(),
                snapshot.getCreatedAt(),
                snapshot.getUpdatedAt()
        );
    }
}

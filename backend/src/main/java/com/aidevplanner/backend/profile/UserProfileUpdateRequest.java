package com.aidevplanner.backend.profile;

public record UserProfileUpdateRequest(
        String preferredLearningStyle,
        String pacePreference,
        String timeBudgetNote,
        String manualCorrection
) {
}

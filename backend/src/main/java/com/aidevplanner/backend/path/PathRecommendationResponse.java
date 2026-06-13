package com.aidevplanner.backend.path;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PathRecommendationResponse(
        Long id,
        Long goalId,
        Long userId,
        Long sourceAgentRunId,
        Integer version,
        String recommendedPath,
        String summary,
        String currentPosition,
        String nextStep,
        String difficulty,
        Integer durationDays,
        BigDecimal dailyTimeHours,
        List<String> focusAreas,
        List<String> milestones,
        List<String> riskSignals,
        List<String> evidence,
        List<String> finalDeliverables,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static PathRecommendationResponse from(PathRecommendation recommendation) {
        Long sourceAgentRunId = recommendation.getSourceAgentRun() == null
                ? null
                : recommendation.getSourceAgentRun().getId();
        return new PathRecommendationResponse(
                recommendation.getId(),
                recommendation.getGoal().getId(),
                recommendation.getUser().getId(),
                sourceAgentRunId,
                recommendation.getVersion(),
                recommendation.getPathTitle(),
                recommendation.getSummary(),
                recommendation.getCurrentPosition(),
                recommendation.getNextStep(),
                recommendation.getDifficulty(),
                recommendation.getDurationDays(),
                recommendation.getDailyTimeHours(),
                recommendation.getFocusAreas(),
                recommendation.getMilestones(),
                recommendation.getRiskSignals(),
                recommendation.getEvidence(),
                recommendation.getFinalDeliverables(),
                recommendation.getCreatedAt(),
                recommendation.getUpdatedAt()
        );
    }
}

package com.aidevplanner.backend.path;

import com.aidevplanner.backend.knowledge.KnowledgeBasisResponse;

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
        KnowledgeBasisResponse knowledgeBasis,
        List<String> finalDeliverables,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static PathRecommendationResponse from(
            PathRecommendation recommendation,
            KnowledgeBasisResponse knowledgeBasis
    ) {
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
                knowledgeBasis,
                recommendation.getFinalDeliverables(),
                recommendation.getCreatedAt(),
                recommendation.getUpdatedAt()
        );
    }
}

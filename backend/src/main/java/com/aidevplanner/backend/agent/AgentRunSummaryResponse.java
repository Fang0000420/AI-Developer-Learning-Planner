package com.aidevplanner.backend.agent;

import java.time.LocalDateTime;

public record AgentRunSummaryResponse(
        Long id,
        Long userId,
        Long goalId,
        Long planId,
        String agentName,
        AgentRunStatus status,
        Long latencyMs,
        String errorMessage,
        String requestId,
        LocalDateTime createdAt
) {
}

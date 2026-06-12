package com.aidevplanner.backend.agent;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDateTime;

public record AgentRunDetailResponse(
        Long id,
        Long userId,
        Long goalId,
        Long planId,
        String agentName,
        AgentRunStatus status,
        AgentResponseSource responseSource,
        Long latencyMs,
        String errorMessage,
        String requestId,
        JsonNode inputJson,
        JsonNode outputJson,
        LocalDateTime createdAt
) {
}

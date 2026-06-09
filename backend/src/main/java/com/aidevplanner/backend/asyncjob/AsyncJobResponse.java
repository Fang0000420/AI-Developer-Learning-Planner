package com.aidevplanner.backend.asyncjob;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.util.UUID;

public record AsyncJobResponse(
        UUID jobId,
        AsyncJobType jobType,
        AsyncJobStatus status,
        JsonNode result,
        String errorMessage,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

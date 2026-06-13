package com.aidevplanner.backend.knowledge;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record KnowledgeIngestionRequest(
        @NotNull(message = "Knowledge document id is required.")
        @Positive(message = "Knowledge document id must be positive.")
        Long documentId
) {
}

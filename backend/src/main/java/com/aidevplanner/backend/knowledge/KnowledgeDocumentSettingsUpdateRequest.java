package com.aidevplanner.backend.knowledge;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record KnowledgeDocumentSettingsUpdateRequest(
        @NotNull(message = "Knowledge document scope is required.")
        KnowledgeDocumentScope scope,
        @NotNull(message = "Retrieval priority is required.")
        @Min(value = 1, message = "Retrieval priority must be between 1 and 5.")
        @Max(value = 5, message = "Retrieval priority must be between 1 and 5.")
        Integer retrievalPriority
) {
}

package com.aidevplanner.backend.knowledge;

import jakarta.validation.constraints.NotNull;

public record KnowledgeDocumentEnabledUpdateRequest(
        @NotNull(message = "Enabled is required.")
        Boolean enabled
) {
}

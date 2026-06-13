package com.aidevplanner.backend.goal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GoalKnowledgePreferenceService {

    private final ObjectMapper objectMapper;

    public GoalKnowledgePreferenceService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public GoalKnowledgePreference read(Goal goal) {
        if (goal.getKnowledgePreferenceJson() == null || goal.getKnowledgePreferenceJson().isBlank()) {
            return defaultPreference();
        }
        try {
            GoalKnowledgePreference parsed = objectMapper.readValue(
                    goal.getKnowledgePreferenceJson(),
                    GoalKnowledgePreference.class
            );
            return normalize(parsed);
        } catch (JsonProcessingException ignored) {
            return defaultPreference();
        }
    }

    public GoalKnowledgePreferenceResponse response(Goal goal) {
        GoalKnowledgePreference preference = read(goal);
        return new GoalKnowledgePreferenceResponse(
                goal.getId(),
                preference.preferredDocumentIds(),
                preference.preferredScope(),
                preference.preferredCategories()
        );
    }

    public void write(Goal goal, GoalKnowledgePreferenceRequest request) {
        GoalKnowledgePreference normalized = normalize(new GoalKnowledgePreference(
                request.preferredDocumentIds(),
                request.preferredScope(),
                request.preferredCategories()
        ));
        try {
            goal.setKnowledgePreferenceJson(objectMapper.writeValueAsString(normalized));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to persist goal knowledge preference.", exception);
        }
    }

    private GoalKnowledgePreference normalize(GoalKnowledgePreference preference) {
        return new GoalKnowledgePreference(
                preference.preferredDocumentIds() == null
                        ? List.of()
                        : preference.preferredDocumentIds().stream().filter(id -> id != null && id > 0).distinct().toList(),
                normalizeScope(preference.preferredScope()),
                preference.preferredCategories() == null
                        ? List.of()
                        : preference.preferredCategories().stream()
                        .filter(value -> value != null && !value.isBlank())
                        .map(String::trim)
                        .distinct()
                        .toList()
        );
    }

    private String normalizeScope(String preferredScope) {
        if (preferredScope == null || preferredScope.isBlank()) {
            return null;
        }
        String normalized = preferredScope.trim().toUpperCase();
        return switch (normalized) {
            case "PERSONAL", "PLATFORM" -> normalized;
            default -> null;
        };
    }

    private GoalKnowledgePreference defaultPreference() {
        return new GoalKnowledgePreference(List.of(), null, List.of());
    }
}

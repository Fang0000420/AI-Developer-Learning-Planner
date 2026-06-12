package com.aidevplanner.backend.agent;

public enum AgentResponseSource {
    MODEL,
    FALLBACK;

    public static AgentResponseSource fromHeader(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return switch (value.trim().toLowerCase()) {
            case "model" -> MODEL;
            case "fallback" -> FALLBACK;
            default -> null;
        };
    }
}

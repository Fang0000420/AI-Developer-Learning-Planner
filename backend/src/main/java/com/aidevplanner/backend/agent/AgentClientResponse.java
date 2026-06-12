package com.aidevplanner.backend.agent;

public record AgentClientResponse<T>(T payload, AgentResponseSource responseSource) {

    public static <T> AgentClientResponse<T> model(T payload) {
        return new AgentClientResponse<>(payload, AgentResponseSource.MODEL);
    }

    public static <T> AgentClientResponse<T> fallback(T payload) {
        return new AgentClientResponse<>(payload, AgentResponseSource.FALLBACK);
    }
}

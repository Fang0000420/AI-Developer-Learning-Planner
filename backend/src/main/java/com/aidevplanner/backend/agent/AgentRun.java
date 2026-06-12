package com.aidevplanner.backend.agent;

import com.aidevplanner.backend.goal.Goal;
import com.aidevplanner.backend.learningplan.LearningPlan;
import com.aidevplanner.backend.observability.ObservabilityContext;
import com.aidevplanner.backend.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@EntityListeners(AgentRunLogListener.class)
@Table(name = "agent_runs")
public class AgentRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "goal_id")
    private Goal goal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id")
    private LearningPlan plan;

    @Column(name = "agent_name", nullable = false, length = 100)
    private String agentName;

    @Column(name = "input_json", nullable = false, columnDefinition = "TEXT")
    private String inputJson;

    @Column(name = "output_json", columnDefinition = "TEXT")
    private String outputJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AgentRunStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "response_source", length = 30)
    private AgentResponseSource responseSource;

    @Column(name = "latency_ms", nullable = false)
    private Long latencyMs;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "request_id", length = 100)
    private String requestId;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected AgentRun() {
    }

    public AgentRun(
            User user,
            Goal goal,
            String agentName,
            String inputJson,
            String outputJson,
            AgentRunStatus status,
            Long latencyMs,
            String errorMessage
    ) {
        this(user, goal, agentName, inputJson, outputJson, status, latencyMs, null, errorMessage);
    }

    public AgentRun(
            User user,
            Goal goal,
            String agentName,
            String inputJson,
            String outputJson,
            AgentRunStatus status,
            Long latencyMs,
            AgentResponseSource responseSource,
            String errorMessage
    ) {
        this.user = user;
        this.goal = goal;
        this.agentName = agentName;
        this.inputJson = inputJson;
        this.outputJson = outputJson;
        this.status = status;
        this.responseSource = responseSource;
        this.latencyMs = latencyMs;
        this.errorMessage = errorMessage;
        this.requestId = ObservabilityContext.getRequestId();
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Goal getGoal() {
        return goal;
    }

    public LearningPlan getPlan() {
        return plan;
    }

    public void setPlan(LearningPlan plan) {
        this.plan = plan;
    }

    public String getAgentName() {
        return agentName;
    }

    public String getInputJson() {
        return inputJson;
    }

    public String getOutputJson() {
        return outputJson;
    }

    public AgentRunStatus getStatus() {
        return status;
    }

    public AgentResponseSource getResponseSource() {
        return responseSource;
    }

    public Long getLatencyMs() {
        return latencyMs;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getRequestId() {
        return requestId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}

package com.aidevplanner.backend.path;

import com.aidevplanner.backend.agent.AgentRun;
import com.aidevplanner.backend.goal.Goal;
import com.aidevplanner.backend.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "path_recommendations")
public class PathRecommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "goal_id", nullable = false)
    private Goal goal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_agent_run_id")
    private AgentRun sourceAgentRun;

    @Column(nullable = false)
    private Integer version;

    @Column(name = "path_title", nullable = false, length = 255)
    private String pathTitle;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String summary;

    @Column(name = "current_position", nullable = false, columnDefinition = "TEXT")
    private String currentPosition;

    @Column(name = "next_step", nullable = false, columnDefinition = "TEXT")
    private String nextStep;

    @Column(nullable = false, length = 50)
    private String difficulty;

    @Column(name = "duration_days", nullable = false)
    private Integer durationDays;

    @Column(name = "daily_time_hours", nullable = false, precision = 5, scale = 2)
    private BigDecimal dailyTimeHours;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "focus_areas", nullable = false, columnDefinition = "jsonb")
    private List<String> focusAreas;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private List<String> milestones;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "risk_signals", nullable = false, columnDefinition = "jsonb")
    private List<String> riskSignals;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private List<String> evidence;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "final_deliverables", nullable = false, columnDefinition = "jsonb")
    private List<String> finalDeliverables;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    protected PathRecommendation() {
    }

    public PathRecommendation(
            User user,
            Goal goal,
            AgentRun sourceAgentRun,
            Integer version,
            String pathTitle,
            String summary,
            String currentPosition,
            String nextStep,
            String difficulty,
            Integer durationDays,
            BigDecimal dailyTimeHours,
            List<String> focusAreas,
            List<String> milestones,
            List<String> riskSignals,
            List<String> evidence,
            List<String> finalDeliverables
    ) {
        this.user = user;
        this.goal = goal;
        this.sourceAgentRun = sourceAgentRun;
        this.version = version;
        this.pathTitle = pathTitle;
        this.summary = summary;
        this.currentPosition = currentPosition;
        this.nextStep = nextStep;
        this.difficulty = difficulty;
        this.durationDays = durationDays;
        this.dailyTimeHours = dailyTimeHours;
        this.focusAreas = focusAreas;
        this.milestones = milestones;
        this.riskSignals = riskSignals;
        this.evidence = evidence;
        this.finalDeliverables = finalDeliverables;
    }

    @PrePersist
    void onCreate() {
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
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

    public AgentRun getSourceAgentRun() {
        return sourceAgentRun;
    }

    public Integer getVersion() {
        return version;
    }

    public String getPathTitle() {
        return pathTitle;
    }

    public String getSummary() {
        return summary;
    }

    public String getCurrentPosition() {
        return currentPosition;
    }

    public String getNextStep() {
        return nextStep;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public Integer getDurationDays() {
        return durationDays;
    }

    public BigDecimal getDailyTimeHours() {
        return dailyTimeHours;
    }

    public List<String> getFocusAreas() {
        return focusAreas;
    }

    public List<String> getMilestones() {
        return milestones;
    }

    public List<String> getRiskSignals() {
        return riskSignals;
    }

    public List<String> getEvidence() {
        return evidence;
    }

    public List<String> getFinalDeliverables() {
        return finalDeliverables;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}

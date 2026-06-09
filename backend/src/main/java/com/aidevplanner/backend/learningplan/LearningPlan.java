package com.aidevplanner.backend.learningplan;

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

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "learning_plans")
public class LearningPlan {

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

    @Column(name = "plan_title", nullable = false, length = 255)
    private String planTitle;

    @Column(name = "duration_days", nullable = false)
    private Integer durationDays;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "plan_json", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> planJson;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    protected LearningPlan() {
    }

    public LearningPlan(
            User user,
            Goal goal,
            AgentRun sourceAgentRun,
            String planTitle,
            Integer durationDays,
            Map<String, Object> planJson
    ) {
        this.user = user;
        this.goal = goal;
        this.sourceAgentRun = sourceAgentRun;
        this.planTitle = planTitle;
        this.durationDays = durationDays;
        this.planJson = planJson;
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

    public String getPlanTitle() {
        return planTitle;
    }

    public Integer getDurationDays() {
        return durationDays;
    }

    public Map<String, Object> getPlanJson() {
        return planJson;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}

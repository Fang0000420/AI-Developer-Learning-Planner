package com.aidevplanner.backend.progress;

import com.aidevplanner.backend.goal.Goal;
import com.aidevplanner.backend.learningplan.LearningPlan;
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
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "progress_logs")
public class ProgressLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private LearningPlan plan;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "goal_id", nullable = false)
    private Goal goal;

    @Column(name = "day_index", nullable = false)
    private Integer dayIndex;

    @Column(name = "user_feedback", nullable = false, columnDefinition = "TEXT")
    private String userFeedback;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "completed_task_ids", nullable = false, columnDefinition = "jsonb")
    private List<Long> completedTaskIds;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "unfinished_task_ids", nullable = false, columnDefinition = "jsonb")
    private List<Long> unfinishedTaskIds;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private List<String> blockers;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "review_result_json", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> reviewResultJson;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    protected ProgressLog() {
    }

    public ProgressLog(
            LearningPlan plan,
            User user,
            Goal goal,
            Integer dayIndex,
            String userFeedback,
            List<Long> completedTaskIds,
            List<Long> unfinishedTaskIds,
            List<String> blockers,
            Map<String, Object> reviewResultJson
    ) {
        this.plan = plan;
        this.user = user;
        this.goal = goal;
        this.dayIndex = dayIndex;
        this.userFeedback = userFeedback;
        this.completedTaskIds = completedTaskIds;
        this.unfinishedTaskIds = unfinishedTaskIds;
        this.blockers = blockers;
        this.reviewResultJson = reviewResultJson;
    }

    @PrePersist
    void onCreate() {
        if (completedTaskIds == null) {
            completedTaskIds = List.of();
        }
        if (unfinishedTaskIds == null) {
            unfinishedTaskIds = List.of();
        }
        if (blockers == null) {
            blockers = List.of();
        }
        if (reviewResultJson == null) {
            reviewResultJson = Map.of();
        }
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

    public LearningPlan getPlan() {
        return plan;
    }

    public User getUser() {
        return user;
    }

    public Goal getGoal() {
        return goal;
    }

    public Integer getDayIndex() {
        return dayIndex;
    }

    public String getUserFeedback() {
        return userFeedback;
    }

    public List<Long> getCompletedTaskIds() {
        return completedTaskIds;
    }

    public List<Long> getUnfinishedTaskIds() {
        return unfinishedTaskIds;
    }

    public List<String> getBlockers() {
        return blockers;
    }

    public Map<String, Object> getReviewResultJson() {
        return reviewResultJson;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}

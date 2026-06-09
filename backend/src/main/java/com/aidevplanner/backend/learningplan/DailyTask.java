package com.aidevplanner.backend.learningplan;

import com.aidevplanner.backend.goal.Goal;
import com.aidevplanner.backend.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "daily_tasks")
public class DailyTask {

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

    @Column(name = "task_order", nullable = false)
    private Integer taskOrder;

    @Column(name = "day_theme", nullable = false, columnDefinition = "TEXT")
    private String dayTheme;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "estimated_minutes", nullable = false)
    private Integer estimatedMinutes;

    @Column(nullable = false, length = 50)
    private String type;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String deliverable;

    @Column(nullable = false, length = 50)
    private String priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private DailyTaskStatus status = DailyTaskStatus.PENDING;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    protected DailyTask() {
    }

    public DailyTask(
            LearningPlan plan,
            User user,
            Goal goal,
            Integer dayIndex,
            Integer taskOrder,
            String dayTheme,
            String title,
            String description,
            Integer estimatedMinutes,
            String type,
            String deliverable,
            String priority
    ) {
        this.plan = plan;
        this.user = user;
        this.goal = goal;
        this.dayIndex = dayIndex;
        this.taskOrder = taskOrder;
        this.dayTheme = dayTheme;
        this.title = title;
        this.description = description;
        this.estimatedMinutes = estimatedMinutes;
        this.type = type;
        this.deliverable = deliverable;
        this.priority = priority;
    }

    @PrePersist
    void onCreate() {
        if (status == null) {
            status = DailyTaskStatus.PENDING;
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

    public Integer getTaskOrder() {
        return taskOrder;
    }

    public String getDayTheme() {
        return dayTheme;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Integer getEstimatedMinutes() {
        return estimatedMinutes;
    }

    public String getType() {
        return type;
    }

    public String getDeliverable() {
        return deliverable;
    }

    public String getPriority() {
        return priority;
    }

    public DailyTaskStatus getStatus() {
        return status;
    }

    public void setStatus(DailyTaskStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}

package com.aidevplanner.backend.profile;

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
import java.util.List;

@Entity
@Table(name = "goal_profile_snapshots")
public class GoalProfileSnapshot {

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
    @JoinColumn(name = "skill_profile_id")
    private SkillProfile skillProfile;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_profile_version_id", nullable = false)
    private UserProfileVersion userProfileVersion;

    @Column(name = "snapshot_summary", nullable = false, columnDefinition = "TEXT")
    private String snapshotSummary;

    @Column(name = "preferred_learning_style", length = 120)
    private String preferredLearningStyle;

    @Column(name = "pace_preference", length = 120)
    private String pacePreference;

    @Column(name = "time_budget_note", columnDefinition = "TEXT")
    private String timeBudgetNote;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "current_skills", nullable = false, columnDefinition = "jsonb")
    private List<String> currentSkills;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private List<String> strengths;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private List<String> weaknesses;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "focus_areas", nullable = false, columnDefinition = "jsonb")
    private List<String> focusAreas;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "risk_signals", nullable = false, columnDefinition = "jsonb")
    private List<String> riskSignals;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private List<String> evidence;

    @Column(name = "recommended_direction", nullable = false, columnDefinition = "TEXT")
    private String recommendedDirection;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    protected GoalProfileSnapshot() {
    }

    public GoalProfileSnapshot(
            User user,
            Goal goal,
            SkillProfile skillProfile,
            UserProfileVersion userProfileVersion,
            String snapshotSummary,
            String preferredLearningStyle,
            String pacePreference,
            String timeBudgetNote,
            List<String> currentSkills,
            List<String> strengths,
            List<String> weaknesses,
            List<String> focusAreas,
            List<String> riskSignals,
            List<String> evidence,
            String recommendedDirection
    ) {
        this.user = user;
        this.goal = goal;
        this.skillProfile = skillProfile;
        this.userProfileVersion = userProfileVersion;
        this.snapshotSummary = snapshotSummary;
        this.preferredLearningStyle = preferredLearningStyle;
        this.pacePreference = pacePreference;
        this.timeBudgetNote = timeBudgetNote;
        this.currentSkills = currentSkills;
        this.strengths = strengths;
        this.weaknesses = weaknesses;
        this.focusAreas = focusAreas;
        this.riskSignals = riskSignals;
        this.evidence = evidence;
        this.recommendedDirection = recommendedDirection;
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

    public SkillProfile getSkillProfile() {
        return skillProfile;
    }

    public UserProfileVersion getUserProfileVersion() {
        return userProfileVersion;
    }

    public String getSnapshotSummary() {
        return snapshotSummary;
    }

    public String getPreferredLearningStyle() {
        return preferredLearningStyle;
    }

    public String getPacePreference() {
        return pacePreference;
    }

    public String getTimeBudgetNote() {
        return timeBudgetNote;
    }

    public List<String> getCurrentSkills() {
        return currentSkills;
    }

    public List<String> getStrengths() {
        return strengths;
    }

    public List<String> getWeaknesses() {
        return weaknesses;
    }

    public List<String> getFocusAreas() {
        return focusAreas;
    }

    public List<String> getRiskSignals() {
        return riskSignals;
    }

    public List<String> getEvidence() {
        return evidence;
    }

    public String getRecommendedDirection() {
        return recommendedDirection;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}

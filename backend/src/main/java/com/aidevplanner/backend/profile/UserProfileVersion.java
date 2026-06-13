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
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "user_profile_versions")
public class UserProfileVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_profile_id", nullable = false)
    private UserProfile userProfile;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_goal_id")
    private Goal sourceGoal;

    @Column(nullable = false)
    private Integer version;

    @Column(name = "profile_summary", nullable = false, columnDefinition = "TEXT")
    private String profileSummary;

    @Column(name = "preferred_learning_style", length = 120)
    private String preferredLearningStyle;

    @Column(name = "pace_preference", length = 120)
    private String pacePreference;

    @Column(name = "time_budget_note", columnDefinition = "TEXT")
    private String timeBudgetNote;

    @Column(name = "manual_correction", columnDefinition = "TEXT")
    private String manualCorrection;

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

    protected UserProfileVersion() {
    }

    public UserProfileVersion(
            UserProfile userProfile,
            User user,
            Goal sourceGoal,
            Integer version,
            String profileSummary,
            String preferredLearningStyle,
            String pacePreference,
            String timeBudgetNote,
            String manualCorrection,
            List<String> currentSkills,
            List<String> strengths,
            List<String> weaknesses,
            List<String> focusAreas,
            List<String> riskSignals,
            List<String> evidence,
            String recommendedDirection
    ) {
        this.userProfile = userProfile;
        this.user = user;
        this.sourceGoal = sourceGoal;
        this.version = version;
        this.profileSummary = profileSummary;
        this.preferredLearningStyle = preferredLearningStyle;
        this.pacePreference = pacePreference;
        this.timeBudgetNote = timeBudgetNote;
        this.manualCorrection = manualCorrection;
        this.currentSkills = currentSkills;
        this.strengths = strengths;
        this.weaknesses = weaknesses;
        this.focusAreas = focusAreas;
        this.riskSignals = riskSignals;
        this.evidence = evidence;
        this.recommendedDirection = recommendedDirection;
    }

    public Long getId() {
        return id;
    }

    public UserProfile getUserProfile() {
        return userProfile;
    }

    public User getUser() {
        return user;
    }

    public Goal getSourceGoal() {
        return sourceGoal;
    }

    public Integer getVersion() {
        return version;
    }

    public String getProfileSummary() {
        return profileSummary;
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

    public String getManualCorrection() {
        return manualCorrection;
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
}

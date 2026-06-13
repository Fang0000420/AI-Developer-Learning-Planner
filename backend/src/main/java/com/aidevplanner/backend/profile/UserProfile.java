package com.aidevplanner.backend.profile;

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
@Table(name = "user_profiles")
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "current_version", nullable = false)
    private Integer currentVersion;

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

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    protected UserProfile() {
    }

    public UserProfile(
            User user,
            Integer currentVersion,
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
        this.user = user;
        this.currentVersion = currentVersion;
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

    public Integer getCurrentVersion() {
        return currentVersion;
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void updateFromVersion(UserProfileVersion version) {
        this.currentVersion = version.getVersion();
        this.profileSummary = version.getProfileSummary();
        this.preferredLearningStyle = version.getPreferredLearningStyle();
        this.pacePreference = version.getPacePreference();
        this.timeBudgetNote = version.getTimeBudgetNote();
        this.manualCorrection = version.getManualCorrection();
        this.currentSkills = version.getCurrentSkills();
        this.strengths = version.getStrengths();
        this.weaknesses = version.getWeaknesses();
        this.focusAreas = version.getFocusAreas();
        this.riskSignals = version.getRiskSignals();
        this.evidence = version.getEvidence();
        this.recommendedDirection = version.getRecommendedDirection();
    }
}

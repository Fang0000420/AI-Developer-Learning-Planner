CREATE TABLE user_profiles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    current_version INTEGER NOT NULL,
    profile_summary TEXT NOT NULL,
    preferred_learning_style VARCHAR(120),
    pace_preference VARCHAR(120),
    time_budget_note TEXT,
    manual_correction TEXT,
    current_skills JSONB NOT NULL,
    strengths JSONB NOT NULL,
    weaknesses JSONB NOT NULL,
    focus_areas JSONB NOT NULL,
    risk_signals JSONB NOT NULL,
    evidence JSONB NOT NULL,
    recommended_direction TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_profile_versions (
    id BIGSERIAL PRIMARY KEY,
    user_profile_id BIGINT NOT NULL REFERENCES user_profiles(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    source_goal_id BIGINT REFERENCES goals(id) ON DELETE SET NULL,
    version INTEGER NOT NULL,
    profile_summary TEXT NOT NULL,
    preferred_learning_style VARCHAR(120),
    pace_preference VARCHAR(120),
    time_budget_note TEXT,
    manual_correction TEXT,
    current_skills JSONB NOT NULL,
    strengths JSONB NOT NULL,
    weaknesses JSONB NOT NULL,
    focus_areas JSONB NOT NULL,
    risk_signals JSONB NOT NULL,
    evidence JSONB NOT NULL,
    recommended_direction TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE goal_profile_snapshots (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    goal_id BIGINT NOT NULL REFERENCES goals(id) ON DELETE CASCADE,
    skill_profile_id BIGINT REFERENCES skill_profiles(id) ON DELETE SET NULL,
    user_profile_version_id BIGINT NOT NULL REFERENCES user_profile_versions(id) ON DELETE CASCADE,
    snapshot_summary TEXT NOT NULL,
    preferred_learning_style VARCHAR(120),
    pace_preference VARCHAR(120),
    time_budget_note TEXT,
    current_skills JSONB NOT NULL,
    strengths JSONB NOT NULL,
    weaknesses JSONB NOT NULL,
    focus_areas JSONB NOT NULL,
    risk_signals JSONB NOT NULL,
    evidence JSONB NOT NULL,
    recommended_direction TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uq_user_profiles_user_id ON user_profiles(user_id);
CREATE UNIQUE INDEX uq_user_profile_versions_profile_version ON user_profile_versions(user_profile_id, version);
CREATE INDEX idx_user_profile_versions_user_id ON user_profile_versions(user_id);
CREATE INDEX idx_user_profile_versions_source_goal_id ON user_profile_versions(source_goal_id);
CREATE INDEX idx_goal_profile_snapshots_user_id ON goal_profile_snapshots(user_id);
CREATE INDEX idx_goal_profile_snapshots_goal_id ON goal_profile_snapshots(goal_id);
CREATE INDEX idx_goal_profile_snapshots_version_id ON goal_profile_snapshots(user_profile_version_id);

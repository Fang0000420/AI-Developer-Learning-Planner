CREATE TABLE path_recommendations (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    goal_id BIGINT NOT NULL REFERENCES goals(id) ON DELETE CASCADE,
    source_agent_run_id BIGINT REFERENCES agent_runs(id) ON DELETE SET NULL,
    version INTEGER NOT NULL,
    path_title VARCHAR(255) NOT NULL,
    summary TEXT NOT NULL,
    current_position TEXT NOT NULL,
    next_step TEXT NOT NULL,
    difficulty VARCHAR(50) NOT NULL,
    duration_days INTEGER NOT NULL,
    daily_time_hours NUMERIC(5, 2) NOT NULL,
    focus_areas JSONB NOT NULL,
    milestones JSONB NOT NULL,
    risk_signals JSONB NOT NULL,
    evidence JSONB NOT NULL,
    final_deliverables JSONB NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_path_recommendations_goal_id ON path_recommendations(goal_id);
CREATE INDEX idx_path_recommendations_user_id ON path_recommendations(user_id);
CREATE INDEX idx_path_recommendations_source_agent_run_id ON path_recommendations(source_agent_run_id);
CREATE UNIQUE INDEX uq_path_recommendations_goal_version ON path_recommendations(goal_id, version);

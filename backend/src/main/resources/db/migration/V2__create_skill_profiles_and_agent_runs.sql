CREATE TABLE skill_profiles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    goal_id BIGINT NOT NULL REFERENCES goals(id) ON DELETE CASCADE,
    current_skills JSONB NOT NULL,
    strengths JSONB NOT NULL,
    weaknesses JSONB NOT NULL,
    recommended_direction TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE agent_runs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id),
    goal_id BIGINT REFERENCES goals(id) ON DELETE SET NULL,
    agent_name VARCHAR(100) NOT NULL,
    input_json TEXT NOT NULL,
    output_json TEXT,
    status VARCHAR(50) NOT NULL,
    latency_ms BIGINT NOT NULL,
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_skill_profiles_goal_id ON skill_profiles(goal_id);
CREATE INDEX idx_skill_profiles_user_id ON skill_profiles(user_id);
CREATE INDEX idx_agent_runs_goal_id ON agent_runs(goal_id);
CREATE INDEX idx_agent_runs_agent_name ON agent_runs(agent_name);
CREATE INDEX idx_agent_runs_status ON agent_runs(status);

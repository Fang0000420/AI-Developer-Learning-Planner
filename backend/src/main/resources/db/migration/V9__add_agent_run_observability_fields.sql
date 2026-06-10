ALTER TABLE agent_runs
    ADD COLUMN plan_id BIGINT REFERENCES learning_plans(id) ON DELETE SET NULL,
    ADD COLUMN request_id VARCHAR(100);

CREATE INDEX idx_agent_runs_plan_id ON agent_runs(plan_id);
CREATE INDEX idx_agent_runs_request_id ON agent_runs(request_id);
CREATE INDEX idx_agent_runs_created_at ON agent_runs(created_at);

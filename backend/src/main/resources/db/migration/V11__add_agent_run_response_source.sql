ALTER TABLE agent_runs
    ADD COLUMN response_source VARCHAR(30);

CREATE INDEX idx_agent_runs_response_source ON agent_runs(response_source);

ALTER TABLE goals
    ADD COLUMN response_language VARCHAR(10) NOT NULL DEFAULT 'zh';

CREATE INDEX idx_goals_response_language ON goals(response_language);

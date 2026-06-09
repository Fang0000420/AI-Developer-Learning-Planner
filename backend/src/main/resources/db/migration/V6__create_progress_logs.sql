CREATE TABLE progress_logs (
    id BIGSERIAL PRIMARY KEY,
    plan_id BIGINT NOT NULL REFERENCES learning_plans(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id),
    goal_id BIGINT NOT NULL REFERENCES goals(id) ON DELETE CASCADE,
    day_index INTEGER NOT NULL,
    user_feedback TEXT NOT NULL,
    completed_task_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    unfinished_task_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    blockers JSONB NOT NULL DEFAULT '[]'::jsonb,
    review_result_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_progress_logs_plan_id ON progress_logs(plan_id);
CREATE INDEX idx_progress_logs_goal_id ON progress_logs(goal_id);
CREATE INDEX idx_progress_logs_day_index ON progress_logs(day_index);

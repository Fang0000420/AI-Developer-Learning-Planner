CREATE TABLE learning_plans (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    goal_id BIGINT NOT NULL REFERENCES goals(id) ON DELETE CASCADE,
    source_agent_run_id BIGINT REFERENCES agent_runs(id) ON DELETE SET NULL,
    plan_title VARCHAR(255) NOT NULL,
    duration_days INTEGER NOT NULL,
    plan_json JSONB NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE daily_tasks (
    id BIGSERIAL PRIMARY KEY,
    plan_id BIGINT NOT NULL REFERENCES learning_plans(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id),
    goal_id BIGINT NOT NULL REFERENCES goals(id) ON DELETE CASCADE,
    day_index INTEGER NOT NULL,
    task_order INTEGER NOT NULL,
    day_theme TEXT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    estimated_minutes INTEGER NOT NULL,
    type VARCHAR(50) NOT NULL,
    deliverable TEXT NOT NULL,
    priority VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_learning_plans_goal_id ON learning_plans(goal_id);
CREATE INDEX idx_learning_plans_user_id ON learning_plans(user_id);
CREATE INDEX idx_daily_tasks_plan_id ON daily_tasks(plan_id);
CREATE INDEX idx_daily_tasks_goal_id ON daily_tasks(goal_id);
CREATE INDEX idx_daily_tasks_day_index ON daily_tasks(day_index);

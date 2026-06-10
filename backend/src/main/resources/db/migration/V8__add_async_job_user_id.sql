ALTER TABLE async_jobs
    ADD COLUMN user_id BIGINT REFERENCES users(id);

CREATE INDEX idx_async_jobs_user_id ON async_jobs(user_id);

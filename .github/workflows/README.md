# GitHub Workflows

- `ci.yml`: runs backend Maven tests, agent-service Ruff/Pytest checks, and frontend lint/test/typecheck/format/build checks.
- The backend CI job starts PostgreSQL 16 and Redis 7 services for parity with the project runtime.

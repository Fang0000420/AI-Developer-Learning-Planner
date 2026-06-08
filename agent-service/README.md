# Agent Service

FastAPI AI agent service.

- Planned port: `8000`
- Responsibility: profile analysis, goal decomposition, skill gap analysis, project recommendation, plan generation, review, and adjustment
- Status: Day 03 Python service skeleton

## Project layout

```text
.
|-- app/
|   |-- main.py
|   |-- api/
|   |-- schemas/
|   `-- services/
|-- tests/
`-- pyproject.toml
```

## Development setup

```bash
python -m venv .venv
source .venv/bin/activate
pip install -e ".[dev]"
```

## Basic commands

```bash
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
pytest
ruff check .
```

## Server startup

```bash
cd /home/AI-Developer-Learning-Planner/agent-service
source .venv/bin/activate
uvicorn app.main:app --host 0.0.0.0 --port 8000
```

Verify the health check:

```bash
curl http://localhost:8000/health
```

Verify the profile analyzer stub:

```bash
curl -X POST http://localhost:8000/agent/profile/analyze \
  -H "Content-Type: application/json" \
  -d '{"background":"Backend developer","goal":"Build AI agent apps","dailyAvailableHours":2}'
```

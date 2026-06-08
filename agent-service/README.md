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

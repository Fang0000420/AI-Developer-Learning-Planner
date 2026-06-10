from fastapi.testclient import TestClient
from pytest import MonkeyPatch

from app.main import app
from app.schemas.plan import PlanGenerateRequest, PlanGenerateResponse
from app.services import plan_adjuster, plan_generator

client = TestClient(app)


def test_plan_generate_returns_structured_stub_response(monkeypatch: MonkeyPatch) -> None:
    monkeypatch.setattr(plan_generator, "DEEPSEEK_API_KEY", "")

    response = client.post(
        "/agent/plan/generate",
        json={
            "mainGoal": "Build AI agent apps",
            "currentSkills": ["Java", "Spring Boot"],
            "strengths": ["Backend APIs"],
            "weaknesses": ["LLM evaluation"],
            "subGoals": [
                {
                    "title": "Design agent workflow",
                    "description": "Define planner nodes.",
                    "priority": "high",
                }
            ],
            "skillGaps": [
                {
                    "skill": "Structured LLM output validation",
                    "currentLevel": "beginner",
                    "targetLevel": "intermediate",
                    "priority": "high",
                    "reason": "Needed for schema-safe output.",
                }
            ],
            "recommendedProject": "AI Developer Learning Planner",
            "projectReason": "Covers full-stack AI planner work.",
            "difficulty": "medium-high",
            "coreTechStack": ["Spring Boot", "FastAPI", "Next.js"],
            "finalDeliverables": ["Runnable full-stack demo"],
            "durationDays": 14,
            "dailyAvailableHours": 2,
            "responseLanguage": "en",
        },
    )

    body = response.json()

    assert response.status_code == 200
    assert body["planTitle"]
    assert body["durationDays"] == 14
    assert len(body["days"]) == 14
    assert body["days"][0]["dayIndex"] == 1
    assert body["days"][0]["tasks"]
    assert set(body["days"][0]["tasks"][0]) == {
        "title",
        "description",
        "estimatedMinutes",
        "type",
        "deliverable",
        "priority",
    }
    assert sum(task["estimatedMinutes"] for task in body["days"][0]["tasks"]) <= 120


def test_plan_generate_rejects_unsupported_duration() -> None:
    response = client.post(
        "/agent/plan/generate",
        json={
            "mainGoal": "Build AI agent apps",
            "recommendedProject": "AI Developer Learning Planner",
            "durationDays": 10,
            "dailyAvailableHours": 2,
            "responseLanguage": "en",
        },
    )

    assert response.status_code == 422


def test_plan_model_output_is_normalized_and_padded() -> None:
    request = PlanGenerateRequest(
        mainGoal="Build AI agent apps",
        recommendedProject="AI Developer Learning Planner",
        durationDays=14,
        dailyAvailableHours=1.5,
        responseLanguage="en",
    )
    parsed = {
        "learning_plan": {
            "title": "Two week MVP plan",
            "daily_plans": [
                {
                    "day": 1,
                    "focus": "Setup",
                    "items": [
                        {
                            "name": "Create service skeleton",
                            "details": "Set up the backend and agent directories.",
                            "minutes": "120 minutes",
                            "category": "setup",
                            "output": "Runnable skeleton",
                            "urgency": "urgent",
                        }
                    ],
                }
            ],
        }
    }

    normalized = plan_generator._normalize_model_output(parsed, request)
    response = PlanGenerateResponse.model_validate(normalized)

    assert response.planTitle == "Two week MVP plan"
    assert response.durationDays == 14
    assert len(response.days) == 14
    assert response.days[0].tasks[0].priority == "high"
    assert response.days[0].tasks[0].estimatedMinutes <= 90


def test_plan_model_failure_uses_mock_fallback(monkeypatch: MonkeyPatch) -> None:
    request = PlanGenerateRequest(
        mainGoal="Build AI agent apps",
        recommendedProject="AI Developer Learning Planner",
        durationDays=21,
        dailyAvailableHours=2,
        responseLanguage="en",
    )

    def raise_invalid_response(_request: PlanGenerateRequest) -> PlanGenerateResponse:
        raise ValueError("invalid model response")

    monkeypatch.setattr(plan_generator, "DEEPSEEK_API_KEY", "test-key")
    monkeypatch.setattr(plan_generator, "generate_plan_with_model", raise_invalid_response)

    response = plan_generator.generate_plan(request)

    assert response.durationDays == 21
    assert len(response.days) == 21
    assert response.days[0].tasks


def test_plan_adjust_moves_unfinished_task_to_next_day(monkeypatch: MonkeyPatch) -> None:
    monkeypatch.setattr(plan_adjuster, "DEEPSEEK_API_KEY", "")

    response = client.post(
        "/agent/plan/adjust",
        json={
            "planId": 30,
            "currentDayIndex": 1,
            "currentPlan": [],
            "todayTasks": [
                {
                    "id": 1,
                    "dayIndex": 1,
                    "taskOrder": 1,
                    "title": "Create progress form",
                    "description": "Build frontend submit form.",
                    "estimatedMinutes": 60,
                    "type": "build",
                    "deliverable": "Progress form",
                    "priority": "high",
                    "status": "PENDING",
                }
            ],
            "progressReview": {
                "completedTasks": [],
                "unfinishedTasks": ["Create progress form"],
                "blockers": ["Need UI polish"],
                "impact": "medium",
                "suggestion": "Finish unfinished work first.",
            },
            "unfinishedTasks": [
                {
                    "id": 1,
                    "dayIndex": 1,
                    "taskOrder": 1,
                    "title": "Create progress form",
                    "description": "Build frontend submit form.",
                    "estimatedMinutes": 60,
                    "type": "build",
                    "deliverable": "Progress form",
                    "priority": "high",
                    "status": "PENDING",
                }
            ],
            "nextDayTasks": [],
            "responseLanguage": "en",
        },
    )

    body = response.json()

    assert response.status_code == 200
    assert body["movedTasks"][0]["taskId"] == 1
    assert body["movedTasks"][0]["toDayIndex"] == 2
    assert body["nextDayTasks"][0]["title"] == "Carry over: Create progress form"
    assert body["reason"]


def test_plan_adjust_splits_large_unfinished_task(monkeypatch: MonkeyPatch) -> None:
    monkeypatch.setattr(plan_adjuster, "DEEPSEEK_API_KEY", "")

    response = client.post(
        "/agent/plan/adjust",
        json={
            "planId": 30,
            "currentDayIndex": 2,
            "todayTasks": [
                {
                    "id": 2,
                    "title": "Implement planner adjustment workflow",
                    "description": "Connect the agent and backend flow.",
                    "estimatedMinutes": 120,
                    "type": "build",
                    "deliverable": "Plan adjuster workflow",
                    "priority": "high",
                }
            ],
            "progressReview": {
                "impact": "major",
                "suggestion": "Reduce tomorrow scope.",
            },
            "unfinishedTasks": [
                {
                    "id": 2,
                    "title": "Implement planner adjustment workflow",
                    "description": "Connect the agent and backend flow.",
                    "estimatedMinutes": 120,
                    "type": "build",
                    "deliverable": "Plan adjuster workflow",
                    "priority": "high",
                }
            ],
            "nextDayTasks": [],
            "responseLanguage": "en",
        },
    )

    body = response.json()

    assert response.status_code == 200
    assert body["splitTasks"][0]["sourceTaskId"] == 2
    assert len(body["splitTasks"][0]["parts"]) == 2
    assert sum(part["estimatedMinutes"] for part in body["splitTasks"][0]["parts"]) == 120


def test_plan_generate_returns_chinese_stub_response(monkeypatch: MonkeyPatch) -> None:
    monkeypatch.setattr(plan_generator, "DEEPSEEK_API_KEY", "")

    response = client.post(
        "/agent/plan/generate",
        json={
            "mainGoal": "构建 AI Agent 应用",
            "recommendedProject": "AI Developer Learning Planner",
            "durationDays": 14,
            "dailyAvailableHours": 2,
            "responseLanguage": "zh",
        },
    )

    body = response.json()

    assert response.status_code == 200
    assert "天" in body["planTitle"]
    assert body["days"][0]["tasks"][0]["title"].startswith("梳理")

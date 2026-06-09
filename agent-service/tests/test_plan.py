from fastapi.testclient import TestClient
from pytest import MonkeyPatch

from app.main import app
from app.schemas.plan import PlanGenerateRequest, PlanGenerateResponse
from app.services import plan_generator

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
        },
    )

    assert response.status_code == 422


def test_plan_model_output_is_normalized_and_padded() -> None:
    request = PlanGenerateRequest(
        mainGoal="Build AI agent apps",
        recommendedProject="AI Developer Learning Planner",
        durationDays=14,
        dailyAvailableHours=1.5,
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
    )

    def raise_invalid_response(_request: PlanGenerateRequest) -> PlanGenerateResponse:
        raise ValueError("invalid model response")

    monkeypatch.setattr(plan_generator, "DEEPSEEK_API_KEY", "test-key")
    monkeypatch.setattr(plan_generator, "generate_plan_with_model", raise_invalid_response)

    response = plan_generator.generate_plan(request)

    assert response.durationDays == 21
    assert len(response.days) == 21
    assert response.days[0].tasks

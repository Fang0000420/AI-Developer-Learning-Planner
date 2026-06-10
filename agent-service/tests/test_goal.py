from fastapi.testclient import TestClient
from pytest import MonkeyPatch

from app.main import app
from app.schemas.goal import GoalDecomposeRequest
from app.services import goal_decomposer

client = TestClient(app)


def test_goal_decompose_returns_structured_stub_response(monkeypatch: MonkeyPatch) -> None:
    monkeypatch.setattr(goal_decomposer, "DEEPSEEK_API_KEY", "")

    response = client.post(
        "/agent/goal/decompose",
        json={
            "mainGoal": "Build AI agent apps",
            "background": "Backend developer",
        },
    )

    body = response.json()

    assert response.status_code == 200
    assert set(body) == {"subGoals"}
    assert len(body["subGoals"]) >= 3
    assert set(body["subGoals"][0]) == {"title", "description", "priority"}
    assert body["subGoals"][0]["priority"] in {"high", "medium", "low"}


def test_goal_decompose_rejects_empty_goal() -> None:
    response = client.post(
        "/agent/goal/decompose",
        json={
            "mainGoal": "",
        },
    )

    assert response.status_code == 422


def test_goal_decompose_normalizes_model_aliases() -> None:
    request = GoalDecomposeRequest(
        mainGoal="Build AI agent apps",
        background="Backend developer",
    )
    normalized = goal_decomposer._normalize_model_output(
        {
            "goals": [
                {
                    "name": "Design the agent workflow",
                    "details": "Map the user journey into reliable agent steps.",
                    "urgency": "urgent",
                },
                {
                    "目标": "Implement structured outputs",
                    "说明": "Validate every model response before persistence.",
                    "优先级": "高",
                },
                "Create the frontend review page",
            ]
        },
        request,
    )

    assert len(normalized["subGoals"]) >= 5
    assert normalized["subGoals"][0] == {
        "title": "Design the agent workflow",
        "description": "Map the user journey into reliable agent steps.",
        "priority": "high",
    }
    assert normalized["subGoals"][1]["priority"] == "high"
    assert normalized["subGoals"][2]["title"] == "Create the frontend review page"


def test_goal_decompose_falls_back_when_model_response_is_invalid(
    monkeypatch: MonkeyPatch,
) -> None:
    monkeypatch.setattr(goal_decomposer, "DEEPSEEK_API_KEY", "test-key")

    def fail_model_call(request: GoalDecomposeRequest):
        raise ValueError("invalid model payload")

    monkeypatch.setattr(goal_decomposer, "decompose_goal_with_model", fail_model_call)

    response = goal_decomposer.decompose_goal(
        GoalDecomposeRequest(
            mainGoal="Build AI agent apps",
            background="Backend developer",
        )
    )

    assert len(response.subGoals) >= 5
    assert response.subGoals[0].priority == "high"

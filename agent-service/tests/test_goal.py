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
            "mainGoal": "Improve business English speaking",
            "background": "Customer support specialist",
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
        mainGoal="Improve business English speaking",
        background="Customer support specialist",
    )
    normalized = goal_decomposer._normalize_model_output(
        {
            "goals": [
                {
                    "name": "Build a daily speaking routine",
                    "details": "Practice short speaking drills with feedback every day.",
                    "urgency": "urgent",
                },
                {
                    "目标": "整理高频商务表达",
                    "说明": "收集并反复练习工作中常用的关键表达方式。",
                    "优先级": "高",
                },
                "Record one short speaking sample each week",
            ]
        },
        request,
    )

    assert len(normalized["subGoals"]) >= 5
    assert normalized["subGoals"][0] == {
        "title": "Build a daily speaking routine",
        "description": "Practice short speaking drills with feedback every day.",
        "priority": "high",
    }
    assert normalized["subGoals"][1]["priority"] == "high"
    assert normalized["subGoals"][2]["title"] == "Record one short speaking sample each week"


def test_goal_decompose_localizes_bare_string_description_for_chinese() -> None:
    request = GoalDecomposeRequest(
        mainGoal="提升商务英语口语",
        background="客服专员",
        responseLanguage="zh",
    )

    normalized = goal_decomposer._normalize_model_output(
        {"goals": ["建立每日口语练习节奏"]},
        request,
    )

    assert normalized["subGoals"][0]["title"] == "建立每日口语练习节奏"
    assert "围绕目标" in normalized["subGoals"][0]["description"]


def test_goal_decompose_limits_result_to_eight_items() -> None:
    request = GoalDecomposeRequest(
        mainGoal="Improve business English speaking",
        background="Customer support specialist",
        responseLanguage="en",
    )

    normalized = goal_decomposer._normalize_model_output(
        {
            "goals": [
                {"name": f"Goal {index}", "details": f"Description {index}"}
                for index in range(1, 11)
            ]
        },
        request,
    )

    assert len(normalized["subGoals"]) == 8


def test_goal_decompose_falls_back_when_model_response_is_invalid(
    monkeypatch: MonkeyPatch,
) -> None:
    monkeypatch.setattr(goal_decomposer, "DEEPSEEK_API_KEY", "test-key")

    def fail_model_call(request: GoalDecomposeRequest):
        raise ValueError("invalid model payload")

    monkeypatch.setattr(goal_decomposer, "decompose_goal_with_model", fail_model_call)

    response = goal_decomposer.decompose_goal(
        GoalDecomposeRequest(
            mainGoal="Improve business English speaking",
            background="Customer support specialist",
        )
    )

    assert len(response.subGoals) >= 5
    assert response.subGoals[0].priority == "high"
    assert "FastAPI" not in response.subGoals[0].description

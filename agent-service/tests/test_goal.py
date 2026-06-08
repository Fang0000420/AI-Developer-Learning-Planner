from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)


def test_goal_decompose_returns_structured_stub_response() -> None:
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

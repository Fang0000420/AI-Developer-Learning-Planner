from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)


def test_skill_gap_analyze_returns_structured_stub_response() -> None:
    response = client.post(
        "/agent/skill-gap/analyze",
        json={
            "mainGoal": "Build AI agent apps",
            "currentSkills": ["Python basics", "REST APIs"],
            "strengths": ["Backend foundation"],
            "weaknesses": ["LLM evaluation"],
            "subGoals": [
                {
                    "title": "Design agent workflow",
                    "description": "Define planner nodes.",
                    "priority": "high",
                }
            ],
        },
    )

    body = response.json()

    assert response.status_code == 200
    assert set(body) == {"skillGaps"}
    assert len(body["skillGaps"]) >= 4
    assert set(body["skillGaps"][0]) == {
        "skill",
        "currentLevel",
        "targetLevel",
        "priority",
        "reason",
    }
    assert body["skillGaps"][0]["priority"] in {"high", "medium", "low"}


def test_skill_gap_analyze_rejects_empty_goal() -> None:
    response = client.post(
        "/agent/skill-gap/analyze",
        json={
            "mainGoal": "",
            "currentSkills": [],
            "strengths": [],
            "weaknesses": [],
            "subGoals": [],
        },
    )

    assert response.status_code == 422

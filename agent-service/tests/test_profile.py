from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)


def test_profile_analyze_returns_structured_stub_response() -> None:
    response = client.post(
        "/agent/profile/analyze",
        json={
            "background": "Backend developer",
            "goal": "Build AI agent apps",
            "dailyAvailableHours": 2,
        },
    )

    body = response.json()

    assert response.status_code == 200
    assert set(body) == {
        "currentSkills",
        "strengths",
        "weaknesses",
        "recommendedDirection",
    }
    assert body["currentSkills"]
    assert body["strengths"]
    assert body["weaknesses"]
    assert "Build AI agent apps" in body["recommendedDirection"]

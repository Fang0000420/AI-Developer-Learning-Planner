from fastapi.testclient import TestClient
from pytest import MonkeyPatch

from app.main import app
from app.services import profile_analyzer

client = TestClient(app)


def test_profile_analyze_returns_structured_stub_response(monkeypatch: MonkeyPatch) -> None:
    monkeypatch.setattr(profile_analyzer, "DEEPSEEK_API_KEY", "")

    response = client.post(
        "/agent/profile/analyze",
        json={
            "background": "Backend developer",
            "goal": "Build AI agent apps",
            "dailyAvailableHours": 2,
            "responseLanguage": "en",
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


def test_profile_analyze_returns_chinese_stub_response(monkeypatch: MonkeyPatch) -> None:
    monkeypatch.setattr(profile_analyzer, "DEEPSEEK_API_KEY", "")

    response = client.post(
        "/agent/profile/analyze",
        json={
            "background": "Java 后端开发者",
            "goal": "构建 AI Agent 应用",
            "dailyAvailableHours": 2,
            "responseLanguage": "zh",
        },
    )

    body = response.json()

    assert response.status_code == 200
    assert "构建 AI Agent 应用" in body["recommendedDirection"]
    assert "Python 基础" in body["currentSkills"]

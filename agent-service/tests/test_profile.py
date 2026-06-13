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
            "background": "Customer support specialist with reading habits",
            "goal": "Improve business English speaking",
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
    assert body["currentSkills"][0] == "Goal-related fundamentals"
    assert "Improve business English speaking" in body["recommendedDirection"]


def test_profile_analyze_returns_chinese_stub_response(monkeypatch: MonkeyPatch) -> None:
    monkeypatch.setattr(profile_analyzer, "DEEPSEEK_API_KEY", "")

    response = client.post(
        "/agent/profile/analyze",
        json={
            "background": "有阅读习惯的客服专员",
            "goal": "提升商务英语口语",
            "dailyAvailableHours": 2,
            "responseLanguage": "zh",
        },
    )

    body = response.json()

    assert response.status_code == 200
    assert "提升商务英语口语" in body["recommendedDirection"]
    assert "目标相关基础认知" in body["currentSkills"]


def test_profile_model_output_is_normalized() -> None:
    request = profile_analyzer.ProfileAnalyzeRequest(
        background="Customer support specialist",
        goal="Improve business English speaking",
        dailyAvailableHours=2,
        responseLanguage="en",
    )
    normalized = profile_analyzer._normalize_model_output(
        {
            "analysis": {
                "skills": ["Reading comprehension", "Basic vocabulary"],
                "advantages": ["Consistent study habit"],
                "gaps": ["Speaking fluency"],
                "direction": "Build a steady speaking routine with weekly review.",
            }
        },
        request,
    )

    assert normalized["currentSkills"] == ["Reading comprehension", "Basic vocabulary"]
    assert normalized["strengths"] == ["Consistent study habit"]
    assert normalized["weaknesses"] == ["Speaking fluency"]
    assert (
        normalized["recommendedDirection"]
        == "Build a steady speaking routine with weekly review."
    )

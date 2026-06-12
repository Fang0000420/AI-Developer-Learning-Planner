from fastapi.testclient import TestClient
from pytest import MonkeyPatch

from app.main import app
from app.schemas.goal import SubGoal
from app.schemas.project import ProjectRecommendRequest, ProjectRecommendResponse
from app.schemas.skill_gap import SkillGap
from app.services import project_recommender

client = TestClient(app)


def test_project_recommend_returns_structured_stub_response(monkeypatch: MonkeyPatch) -> None:
    monkeypatch.setattr(project_recommender, "DEEPSEEK_API_KEY", "")

    response = client.post(
        "/agent/project/recommend",
        json={
            "mainGoal": "Improve business English speaking",
            "currentSkills": ["Reading comprehension", "Basic vocabulary"],
            "strengths": ["Regular reading habit"],
            "weaknesses": ["Speaking fluency"],
            "subGoals": [
                {
                    "title": "Build a daily speaking routine",
                    "description": "Practice speaking every day with feedback.",
                    "priority": "high",
                }
            ],
            "skillGaps": [
                {
                    "skill": "Speaking fluency",
                    "currentLevel": "beginner",
                    "targetLevel": "intermediate",
                    "priority": "high",
                    "reason": "Needed to communicate more naturally at work.",
                }
            ],
            "durationDays": 21,
            "dailyAvailableHours": 2,
            "responseLanguage": "en",
        },
    )

    body = response.json()

    assert response.status_code == 200
    assert body["recommendedProject"] == "Improve business English speaking learning track"
    assert body["difficulty"]
    assert body["durationDays"] == 21
    assert body["dailyTimeHours"] == 2
    assert body["coreTechStack"]
    assert body["finalDeliverables"]
    assert "Spring Boot" not in body["coreTechStack"]


def test_project_recommend_rejects_invalid_duration() -> None:
    response = client.post(
        "/agent/project/recommend",
        json={
            "mainGoal": "Improve business English speaking",
            "durationDays": 0,
            "dailyAvailableHours": 2,
        },
    )

    assert response.status_code == 422


def test_project_model_output_is_normalized() -> None:
    request = ProjectRecommendRequest(
        mainGoal="Improve business English speaking",
        currentSkills=[],
        strengths=[],
        weaknesses=[],
        subGoals=[
            SubGoal(
                title="Build a daily speaking routine",
                description="Practice speaking every day with feedback.",
                priority="high",
            )
        ],
        skillGaps=[
            SkillGap(
                skill="Speaking fluency",
                currentLevel="beginner",
                targetLevel="intermediate",
                priority="high",
                reason="Needed to handle real work conversations.",
            )
        ],
        durationDays=14,
        dailyAvailableHours=1.5,
    )
    parsed = {
        "recommendation": {
            "projectName": "Business English speaking track",
            "推荐理由": "围绕真实工作场景训练表达与反馈闭环。",
            "难度": "中等",
            "duration": "14",
            "每日时间": "1.5 hours",
            "技术栈": "听力输入, 跟读复述, 情景表达",
            "交付物": ["口语录音", "表达清单"],
        }
    }

    normalized = project_recommender._normalize_model_output(parsed, request)
    response = ProjectRecommendResponse.model_validate(normalized)

    assert response.recommendedProject == "Business English speaking track"
    assert response.durationDays == 14
    assert response.dailyTimeHours == 1.5
    assert response.coreTechStack == ["听力输入", "跟读复述", "情景表达"]
    assert response.finalDeliverables == ["口语录音", "表达清单"]


def test_project_model_failure_uses_mock_fallback(monkeypatch: MonkeyPatch) -> None:
    request = ProjectRecommendRequest(
        mainGoal="Improve business English speaking",
        durationDays=21,
        dailyAvailableHours=2,
        responseLanguage="en",
    )

    def raise_invalid_response(_request: ProjectRecommendRequest) -> ProjectRecommendResponse:
        raise ValueError("invalid model response")

    monkeypatch.setattr(project_recommender, "DEEPSEEK_API_KEY", "test-key")
    monkeypatch.setattr(
        project_recommender,
        "recommend_project_with_model",
        raise_invalid_response,
    )

    response = project_recommender.recommend_project(request)

    assert response.recommendedProject == "Improve business English speaking learning track"
    assert response.coreTechStack
    assert "Spring Boot" not in response.coreTechStack


def test_project_recommend_uses_multi_round_memory(monkeypatch: MonkeyPatch) -> None:
    request = ProjectRecommendRequest(
        mainGoal="Improve business English speaking",
        durationDays=30,
        dailyAvailableHours=2,
        responseLanguage="en",
    )
    calls: list[dict[str, object]] = []

    def fake_chat_completion_json(**kwargs):
        calls.append(kwargs)
        round_index = len(calls)
        if round_index == 1:
            return {
                "learnerSummary": ["Learner wants stronger spoken performance"],
                "constraints": ["2 hours per day"],
                "opportunities": ["Regular reading habit"],
                "difficultySignals": ["Limited speaking confidence"],
            }
        if round_index == 2:
            return {
                "candidates": [
                    {
                        "name": "Business English speaking track",
                        "fit": "high",
                        "risk": "low",
                        "reason": "Matches the goal with repeated speaking practice.",
                    },
                    {
                        "name": "Business writing track",
                        "fit": "medium",
                        "risk": "medium",
                        "reason": "Useful but less aligned with the speaking goal.",
                    },
                ],
                "decisionHints": ["Prefer the speaking-focused track"],
            }
        return {
            "recommendedProject": "Business English speaking track",
            "reason": "Best match for building spoken confidence through repeated practice.",
            "difficulty": "medium",
            "durationDays": 30,
            "dailyTimeHours": 2,
            "coreTechStack": ["Listening input", "Role-play", "Speaking feedback"],
            "finalDeliverables": ["Speaking recordings", "Phrase bank"],
        }

    monkeypatch.setattr(
        project_recommender,
        "chat_completion_json",
        fake_chat_completion_json,
    )

    response = project_recommender.recommend_project_with_model(request)

    assert response.recommendedProject == "Business English speaking track"
    assert response.durationDays == 30
    assert len(calls) == 3
    assert any(message["role"] == "assistant" for message in calls[1]["messages"])
    assert sum(message["role"] == "assistant" for message in calls[2]["messages"]) == 2

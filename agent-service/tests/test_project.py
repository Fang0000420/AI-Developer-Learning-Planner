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
            "durationDays": 21,
            "dailyAvailableHours": 2,
        },
    )

    body = response.json()

    assert response.status_code == 200
    assert body["recommendedProject"] == "AI Developer Learning Planner"
    assert body["difficulty"]
    assert body["durationDays"] == 21
    assert body["dailyTimeHours"] == 2
    assert body["coreTechStack"]
    assert body["finalDeliverables"]


def test_project_recommend_rejects_invalid_duration() -> None:
    response = client.post(
        "/agent/project/recommend",
        json={
            "mainGoal": "Build AI agent apps",
            "durationDays": 0,
            "dailyAvailableHours": 2,
        },
    )

    assert response.status_code == 422


def test_project_model_output_is_normalized() -> None:
    request = ProjectRecommendRequest(
        mainGoal="Build AI agent apps",
        currentSkills=[],
        strengths=[],
        weaknesses=[],
        subGoals=[
            SubGoal(
                title="Design agent workflow",
                description="Define planner nodes.",
                priority="high",
            )
        ],
        skillGaps=[
            SkillGap(
                skill="LLM evaluation",
                currentLevel="beginner",
                targetLevel="intermediate",
                priority="high",
                reason="Needed to assess outputs.",
            )
        ],
        durationDays=14,
        dailyAvailableHours=1.5,
    )
    parsed = {
        "recommendation": {
            "projectName": "AI planner MVP",
            "推荐理由": "Covers agent and full-stack integration.",
            "难度": "中高",
            "duration": "14",
            "每日时间": "1.5 hours",
            "技术栈": "FastAPI, Spring Boot, Next.js",
            "交付物": ["Demo", "README"],
        }
    }

    normalized = project_recommender._normalize_model_output(parsed, request)
    response = ProjectRecommendResponse.model_validate(normalized)

    assert response.recommendedProject == "AI Developer Learning Planner"
    assert response.durationDays == 14
    assert response.dailyTimeHours == 1.5
    assert response.coreTechStack == ["FastAPI", "Spring Boot", "Next.js"]
    assert response.finalDeliverables == ["Demo", "README"]


def test_project_model_failure_uses_mock_fallback(monkeypatch: MonkeyPatch) -> None:
    request = ProjectRecommendRequest(
        mainGoal="Build AI agent apps",
        durationDays=21,
        dailyAvailableHours=2,
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

    assert response.recommendedProject == "AI Developer Learning Planner"
    assert response.coreTechStack

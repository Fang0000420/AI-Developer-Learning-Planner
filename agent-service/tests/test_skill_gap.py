from fastapi.testclient import TestClient
from pytest import MonkeyPatch

from app.main import app
from app.schemas.goal import SubGoal
from app.schemas.skill_gap import SkillGapAnalyzeRequest, SkillGapAnalyzeResponse
from app.services import skill_gap_analyzer

client = TestClient(app)


def test_skill_gap_analyze_returns_structured_stub_response(monkeypatch: MonkeyPatch) -> None:
    monkeypatch.setattr(skill_gap_analyzer, "DEEPSEEK_API_KEY", "")

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


def test_skill_gap_model_output_is_normalized_and_padded() -> None:
    request = SkillGapAnalyzeRequest(
        mainGoal="我想提高我的听力和口语能力",
        currentSkills=[],
        strengths=[],
        weaknesses=["听力理解", "口语表达"],
        subGoals=[
            SubGoal(
                title="Build daily speaking practice",
                description="Practice speaking with feedback.",
                priority="high",
            )
        ],
    )
    parsed = {
        "skillGaps": [
            {
                "skill": "听力理解",
                "current_level": "beginner",
                "target": "intermediate",
                "priority": "高",
            }
        ]
    }

    normalized = skill_gap_analyzer._normalize_model_output(parsed, request)
    response = SkillGapAnalyzeResponse.model_validate(normalized)

    assert len(response.skillGaps) >= 4
    assert response.skillGaps[0].priority == "high"
    assert response.skillGaps[0].targetLevel == "intermediate"
    assert response.skillGaps[0].reason


def test_skill_gap_model_failure_uses_mock_fallback(monkeypatch: MonkeyPatch) -> None:
    request = SkillGapAnalyzeRequest(
        mainGoal="Build AI agent apps",
        currentSkills=[],
        strengths=[],
        weaknesses=[],
        subGoals=[],
    )

    def raise_invalid_response(_request: SkillGapAnalyzeRequest) -> SkillGapAnalyzeResponse:
        raise ValueError("invalid model response")

    monkeypatch.setattr(skill_gap_analyzer, "DEEPSEEK_API_KEY", "test-key")
    monkeypatch.setattr(
        skill_gap_analyzer,
        "analyze_skill_gap_with_model",
        raise_invalid_response,
    )

    response = skill_gap_analyzer.analyze_skill_gap(request)

    assert len(response.skillGaps) >= 4
    assert response.skillGaps[0].priority == "high"


def test_skill_gap_nested_dict_output_is_normalized() -> None:
    request = SkillGapAnalyzeRequest(
        mainGoal="Build AI agent apps",
        currentSkills=[],
        strengths=[],
        weaknesses=[],
        subGoals=[],
    )
    parsed = {
        "data": {
            "skill_gaps": {
                "LLM evaluation": {
                    "current_proficiency": "1",
                    "target_level": "intermediate",
                    "urgency": "urgent",
                    "description": "Needed to judge output quality.",
                }
            }
        }
    }

    normalized = skill_gap_analyzer._normalize_model_output(parsed, request)
    response = SkillGapAnalyzeResponse.model_validate(normalized)

    assert response.skillGaps[0].skill == "LLM evaluation"
    assert response.skillGaps[0].currentLevel == "1"
    assert response.skillGaps[0].priority == "high"

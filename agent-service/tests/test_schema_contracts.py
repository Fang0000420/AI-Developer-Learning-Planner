import pytest
from pydantic import ValidationError

from app.schemas.goal import GoalDecomposeRequest, GoalDecomposeResponse
from app.schemas.plan import (
    PlanAdjustRequest,
    PlanGenerateRequest,
    PlanGenerateResponse,
    PlanTask,
)
from app.schemas.profile import ProfileAnalyzeRequest, ProfileAnalyzeResponse
from app.schemas.progress import ProgressReviewRequest, ProgressReviewResponse
from app.schemas.project import ProjectRecommendRequest, ProjectRecommendResponse
from app.schemas.skill_gap import SkillGapAnalyzeRequest, SkillGapAnalyzeResponse


def test_agent_request_schemas_accept_minimal_valid_payloads() -> None:
    sub_goal = {
        "title": "Build a daily speaking routine",
        "description": "Practice speaking every day with feedback.",
        "priority": "high",
    }
    skill_gap = {
        "skill": "Speaking fluency",
        "currentLevel": "beginner",
        "targetLevel": "intermediate",
        "priority": "high",
        "reason": "Required for more natural conversations.",
    }
    task = {
        "id": 1,
        "title": "Practice interview answers",
        "description": "Complete one focused speaking drill.",
        "estimatedMinutes": 45,
        "type": "practice",
        "deliverable": "Speaking notes",
        "priority": "high",
    }

    assert ProfileAnalyzeRequest(
        background="Customer support specialist",
        goal="Improve business English speaking",
        dailyAvailableHours=2,
    ).responseLanguage == "zh"
    assert ProfileAnalyzeRequest(
        background="Customer support specialist",
        goal="Improve business English speaking",
        dailyAvailableHours=2,
        responseLanguage="en",
    ).responseLanguage == "en"
    assert GoalDecomposeRequest(mainGoal="Improve business English speaking")
    assert SkillGapAnalyzeRequest(
        mainGoal="Improve business English speaking",
        subGoals=[sub_goal],
    )
    assert ProjectRecommendRequest(
        mainGoal="Improve business English speaking",
        skillGaps=[skill_gap],
        durationDays=14,
        dailyAvailableHours=2,
    )
    assert PlanGenerateRequest(
        mainGoal="Improve business English speaking",
        durationDays=14,
        dailyAvailableHours=2,
    )
    assert ProgressReviewRequest(
        dayIndex=1,
        todayTasks=[task],
        userFeedback="Finished one speaking round.",
    )
    assert PlanAdjustRequest(
        planId=30,
        currentDayIndex=1,
        todayTasks=[task],
        progressReview={
            "completedTasks": [],
            "unfinishedTasks": ["Practice interview answers"],
            "blockers": [],
            "impact": "minor",
            "suggestion": "Carry the speaking task into tomorrow.",
        },
        unfinishedTasks=[task],
        nextDayTasks=[],
    )


def test_agent_response_schemas_reject_invalid_contracts() -> None:
    assert ProfileAnalyzeResponse(
        currentSkills=["Reading comprehension"],
        strengths=["Regular learning habit"],
        weaknesses=["Speaking fluency"],
        recommendedDirection="Use focused speaking practice with regular review.",
    )

    with pytest.raises(ValidationError):
        GoalDecomposeResponse(subGoals=[])

    with pytest.raises(ValidationError):
        SkillGapAnalyzeResponse(skillGaps=[])

    with pytest.raises(ValidationError):
        ProjectRecommendResponse(
            recommendedProject="",
            reason="",
            difficulty="medium",
            durationDays=14,
            dailyTimeHours=2,
            coreTechStack=[],
            finalDeliverables=[],
        )

    with pytest.raises(ValidationError):
        PlanGenerateResponse(
            planTitle="Invalid short plan",
            durationDays=14,
            days=[
                {
                    "dayIndex": 1,
                    "theme": "Only one day",
                    "tasks": [
                        {
                            "title": "Task",
                            "description": "Too short for the declared duration.",
                            "estimatedMinutes": 30,
                            "type": "build",
                            "deliverable": "Notes",
                            "priority": "medium",
                        }
                    ],
                }
            ],
        )

    with pytest.raises(ValidationError):
        ProgressReviewResponse(impact="blocked", suggestion="Invalid impact.")


def test_schema_normalizers_keep_ci_contracts_stable() -> None:
    urgent_task = PlanTask(
        title="Finish speaking drill",
        description="Complete the scheduled speaking practice.",
        estimatedMinutes=30,
        type="practice",
        deliverable="Speaking notes",
        priority="urgent",
    )
    progress_request = ProgressReviewRequest(
        dayIndex=1,
        todayTasks=[{"id": 1, "title": "Test"}],
        userFeedback="   Finished one speaking round.   ",
        blockers=["  Need example phrases  ", "", "   "],
    )

    assert urgent_task.priority == "high"
    assert progress_request.userFeedback == "Finished one speaking round."
    assert progress_request.blockers == ["Need example phrases"]

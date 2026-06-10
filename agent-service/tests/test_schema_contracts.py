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
        "title": "Design agent workflow",
        "description": "Define planner nodes.",
        "priority": "high",
    }
    skill_gap = {
        "skill": "Structured output validation",
        "currentLevel": "beginner",
        "targetLevel": "intermediate",
        "priority": "high",
        "reason": "Required for reliable agent responses.",
    }
    task = {
        "id": 1,
        "title": "Create progress form",
        "description": "Build the submission UI.",
        "estimatedMinutes": 45,
        "type": "build",
        "deliverable": "Progress form",
        "priority": "high",
    }

    assert ProfileAnalyzeRequest(
        background="Java backend developer",
        goal="Build AI agent applications",
        dailyAvailableHours=2,
    ).responseLanguage == "zh"
    assert ProfileAnalyzeRequest(
        background="Java backend developer",
        goal="Build AI agent applications",
        dailyAvailableHours=2,
        responseLanguage="en",
    ).responseLanguage == "en"
    assert GoalDecomposeRequest(mainGoal="Build AI agent applications")
    assert SkillGapAnalyzeRequest(
        mainGoal="Build AI agent applications",
        subGoals=[sub_goal],
    )
    assert ProjectRecommendRequest(
        mainGoal="Build AI agent applications",
        skillGaps=[skill_gap],
        durationDays=14,
        dailyAvailableHours=2,
    )
    assert PlanGenerateRequest(
        mainGoal="Build AI agent applications",
        recommendedProject="AI Developer Learning Planner",
        durationDays=14,
        dailyAvailableHours=2,
    )
    assert ProgressReviewRequest(
        dayIndex=1,
        todayTasks=[task],
        userFeedback="Finished backend tests.",
    )
    assert PlanAdjustRequest(
        planId=30,
        currentDayIndex=1,
        todayTasks=[task],
        progressReview={
            "completedTasks": [],
            "unfinishedTasks": ["Create progress form"],
            "blockers": [],
            "impact": "minor",
            "suggestion": "Carry the UI task into tomorrow.",
        },
        unfinishedTasks=[task],
        nextDayTasks=[],
    )


def test_agent_response_schemas_reject_invalid_contracts() -> None:
    assert ProfileAnalyzeResponse(
        currentSkills=["Java"],
        strengths=["Backend APIs"],
        weaknesses=["LLM evaluation"],
        recommendedDirection="Build agent-backed full-stack systems.",
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
        title="Fix failed build",
        description="Repair the failing pipeline.",
        estimatedMinutes=30,
        type="ci",
        deliverable="Green build",
        priority="urgent",
    )
    progress_request = ProgressReviewRequest(
        dayIndex=1,
        todayTasks=[{"id": 1, "title": "Test"}],
        userFeedback="   Finished tests.   ",
        blockers=["  Need server run  ", "", "   "],
    )

    assert urgent_task.priority == "high"
    assert progress_request.userFeedback == "Finished tests."
    assert progress_request.blockers == ["Need server run"]

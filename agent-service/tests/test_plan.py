from fastapi.testclient import TestClient
from pytest import MonkeyPatch

from app.main import app
from app.schemas.plan import PlanGenerateRequest, PlanGenerateResponse
from app.services import plan_adjuster, plan_generator

client = TestClient(app)


def test_plan_generate_returns_structured_stub_response(monkeypatch: MonkeyPatch) -> None:
    monkeypatch.setattr(plan_generator, "DEEPSEEK_API_KEY", "")

    response = client.post(
        "/agent/plan/generate",
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
                    "reason": "Needed to respond more naturally in conversation.",
                }
            ],
            "recommendedProject": "Business English speaking track",
            "projectReason": "Helps turn daily practice into visible speaking progress.",
            "difficulty": "medium",
            "coreTechStack": ["Listening input", "Role-play", "Speaking feedback"],
            "finalDeliverables": ["Speaking recordings"],
            "durationDays": 14,
            "dailyAvailableHours": 2,
            "responseLanguage": "en",
        },
    )

    body = response.json()

    assert response.status_code == 200
    assert body["planTitle"]
    assert body["durationDays"] == 14
    assert len(body["days"]) == 14
    assert body["days"][0]["dayIndex"] == 1
    assert body["days"][0]["tasks"]
    assert set(body["days"][0]["tasks"][0]) == {
        "title",
        "description",
        "estimatedMinutes",
        "type",
        "deliverable",
        "priority",
    }
    assert sum(task["estimatedMinutes"] for task in body["days"][0]["tasks"]) <= 120


def test_plan_generate_rejects_unsupported_duration() -> None:
    response = client.post(
        "/agent/plan/generate",
        json={
            "mainGoal": "Improve business English speaking",
            "durationDays": 6,
            "dailyAvailableHours": 2,
            "responseLanguage": "en",
        },
    )

    assert response.status_code == 422


def test_plan_model_output_is_normalized_and_padded() -> None:
    request = PlanGenerateRequest(
        mainGoal="Improve business English speaking",
        durationDays=14,
        dailyAvailableHours=1.5,
        responseLanguage="en",
    )
    parsed = {
        "learning_plan": {
            "title": "Two week MVP plan",
            "daily_plans": [
                {
                    "day": 1,
                    "focus": "Setup",
                    "items": [
                        {
                            "name": "Create service skeleton",
                            "details": "Set up the backend and agent directories.",
                            "minutes": "120 minutes",
                            "category": "setup",
                            "output": "Runnable skeleton",
                            "urgency": "urgent",
                        }
                    ],
                }
            ],
        }
    }

    normalized = plan_generator._normalize_model_output(parsed, request)
    response = PlanGenerateResponse.model_validate(normalized)

    assert response.planTitle == "Two week MVP plan"
    assert response.durationDays == 14
    assert len(response.days) == 14
    assert response.days[0].tasks[0].priority == "high"
    assert response.days[0].tasks[0].estimatedMinutes <= 90


def test_plan_model_failure_uses_mock_fallback(monkeypatch: MonkeyPatch) -> None:
    request = PlanGenerateRequest(
        mainGoal="Improve business English speaking",
        durationDays=21,
        dailyAvailableHours=2,
        responseLanguage="en",
    )

    def raise_invalid_response(_request: PlanGenerateRequest) -> PlanGenerateResponse:
        raise ValueError("invalid model response")

    monkeypatch.setattr(plan_generator, "DEEPSEEK_API_KEY", "test-key")
    monkeypatch.setattr(plan_generator, "generate_plan_with_model", raise_invalid_response)

    response = plan_generator.generate_plan(request)

    assert response.durationDays == 21
    assert len(response.days) == 21
    assert response.days[0].tasks
    assert "Learning Plan" in response.planTitle


def test_plan_adjust_moves_unfinished_task_to_next_day(monkeypatch: MonkeyPatch) -> None:
    monkeypatch.setattr(plan_adjuster, "DEEPSEEK_API_KEY", "")

    response = client.post(
        "/agent/plan/adjust",
        json={
            "planId": 30,
            "currentDayIndex": 1,
            "currentPlan": [],
            "todayTasks": [
                {
                    "id": 1,
                    "dayIndex": 1,
                    "taskOrder": 1,
                    "title": "Practice interview answers",
                    "description": "Complete the next round of spoken answer practice.",
                    "estimatedMinutes": 60,
                    "type": "practice",
                    "deliverable": "Interview answer notes",
                    "priority": "high",
                    "status": "PENDING",
                }
            ],
            "progressReview": {
                "completedTasks": [],
                "unfinishedTasks": ["Practice interview answers"],
                "blockers": ["Need example phrases"],
                "impact": "medium",
                "suggestion": "Finish the unfinished practice first.",
            },
            "unfinishedTasks": [
                {
                    "id": 1,
                    "dayIndex": 1,
                    "taskOrder": 1,
                    "title": "Practice interview answers",
                    "description": "Complete the next round of spoken answer practice.",
                    "estimatedMinutes": 60,
                    "type": "practice",
                    "deliverable": "Interview answer notes",
                    "priority": "high",
                    "status": "PENDING",
                }
            ],
            "nextDayTasks": [],
            "responseLanguage": "en",
        },
    )

    body = response.json()

    assert response.status_code == 200
    assert body["movedTasks"][0]["taskId"] == 1
    assert body["movedTasks"][0]["toDayIndex"] == 2
    assert body["nextDayTasks"][0]["title"] == "Carry over: Practice interview answers"
    assert body["reason"]


def test_plan_adjust_splits_large_unfinished_task(monkeypatch: MonkeyPatch) -> None:
    monkeypatch.setattr(plan_adjuster, "DEEPSEEK_API_KEY", "")

    response = client.post(
        "/agent/plan/adjust",
        json={
            "planId": 30,
            "currentDayIndex": 2,
            "todayTasks": [
                {
                    "id": 2,
                    "title": "Complete speaking simulation set",
                    "description": "Finish all scenario-based speaking simulations.",
                    "estimatedMinutes": 120,
                    "type": "practice",
                    "deliverable": "Simulation notes",
                    "priority": "high",
                }
            ],
            "progressReview": {
                "impact": "major",
                "suggestion": "Reduce tomorrow scope.",
            },
            "unfinishedTasks": [
                {
                    "id": 2,
                    "title": "Complete speaking simulation set",
                    "description": "Finish all scenario-based speaking simulations.",
                    "estimatedMinutes": 120,
                    "type": "practice",
                    "deliverable": "Simulation notes",
                    "priority": "high",
                }
            ],
            "nextDayTasks": [],
            "responseLanguage": "en",
        },
    )

    body = response.json()

    assert response.status_code == 200
    assert body["splitTasks"][0]["sourceTaskId"] == 2
    assert len(body["splitTasks"][0]["parts"]) == 2
    assert sum(part["estimatedMinutes"] for part in body["splitTasks"][0]["parts"]) == 120


def test_plan_generate_returns_chinese_stub_response(monkeypatch: MonkeyPatch) -> None:
    monkeypatch.setattr(plan_generator, "DEEPSEEK_API_KEY", "")

    response = client.post(
        "/agent/plan/generate",
        json={
            "mainGoal": "提升商务英语口语",
            "durationDays": 14,
            "dailyAvailableHours": 2,
            "responseLanguage": "zh",
        },
    )

    body = response.json()

    assert response.status_code == 200
    assert "学习计划" in body["planTitle"]
    assert body["days"][0]["tasks"][0]["title"].startswith("梳理")


def test_plan_generate_accepts_custom_duration_with_stub(monkeypatch: MonkeyPatch) -> None:
    monkeypatch.setattr(plan_generator, "DEEPSEEK_API_KEY", "")

    response = client.post(
        "/agent/plan/generate",
        json={
            "mainGoal": "Improve business English speaking",
            "durationDays": 30,
            "dailyAvailableHours": 2,
            "responseLanguage": "en",
        },
    )

    body = response.json()

    assert response.status_code == 200
    assert body["durationDays"] == 30
    assert len(body["days"]) == 30


def test_plan_generate_uses_chunked_rounds_and_memory(monkeypatch: MonkeyPatch) -> None:
    request = PlanGenerateRequest(
        mainGoal="Improve business English speaking",
        recommendedProject="Business English speaking track",
        durationDays=7,
        dailyAvailableHours=2,
        responseLanguage="en",
    )
    calls: list[dict[str, object]] = []

    def fake_chat_completion_json(**kwargs):
        calls.append(kwargs)
        round_index = len(calls)
        start_day = (round_index - 1) * 2 + 1
        end_day = min(7, start_day + 1)
        days = []
        for day_index in range(start_day, end_day + 1):
            days.append(
                {
                    "dayIndex": day_index,
                    "theme": f"Theme {day_index}",
                    "tasks": [
                        {
                            "title": f"Task {day_index}",
                            "description": f"Do work for day {day_index}.",
                            "estimatedMinutes": 60,
                            "type": "build",
                            "deliverable": f"Deliverable {day_index}",
                            "priority": "high",
                        }
                    ],
                }
            )
        return {
            "planTitle": "Seven-Day Speaking Plan",
            "days": days,
            "memory": {
                "completedDayIndexes": list(range(1, end_day + 1)),
                "establishedThemes": [f"Theme {day_index}" for day_index in range(1, end_day + 1)],
                "carryForwardConstraints": ["Keep scope focused"],
                "nextFocusHints": [f"Continue after day {end_day}"],
            },
        }

    monkeypatch.setattr(plan_generator, "chat_completion_json", fake_chat_completion_json)

    response = plan_generator.generate_plan_with_model(request)

    assert response.durationDays == 7
    assert len(response.days) == 7
    assert len(calls) == 4
    assert calls[0]["model"] == plan_generator.PLAN_GENERATOR_MODEL
    assert not any(message["role"] == "assistant" for message in calls[0]["messages"])
    assert any(message["role"] == "assistant" for message in calls[1]["messages"])
    assert any(
        "previousMemory" in message["content"]
        for message in calls[1]["messages"]
        if message["role"] == "user"
    )


def test_plan_generate_preserves_completed_chunks_when_later_chunk_fails(
    monkeypatch: MonkeyPatch,
) -> None:
    request = PlanGenerateRequest(
        mainGoal="Improve business English speaking",
        recommendedProject="Business English speaking track",
        durationDays=7,
        dailyAvailableHours=2,
        responseLanguage="en",
    )
    call_count = 0

    def flaky_chat_completion_json(**_kwargs):
        nonlocal call_count
        call_count += 1
        if call_count == 1:
            return {
                "planTitle": "Seven-Day Speaking Plan",
                "days": [
                    {
                        "dayIndex": 1,
                        "theme": "Theme 1",
                        "tasks": [
                            {
                                "title": "Task 1",
                                "description": "Do work for day 1.",
                                "estimatedMinutes": 60,
                                "type": "build",
                                "deliverable": "Deliverable 1",
                                "priority": "high",
                            }
                        ],
                    },
                    {
                        "dayIndex": 2,
                        "theme": "Theme 2",
                        "tasks": [
                            {
                                "title": "Task 2",
                                "description": "Do work for day 2.",
                                "estimatedMinutes": 60,
                                "type": "build",
                                "deliverable": "Deliverable 2",
                                "priority": "high",
                            }
                        ],
                    },
                ],
                "memory": {
                    "completedDayIndexes": [1, 2],
                    "establishedThemes": ["Theme 1", "Theme 2"],
                    "carryForwardConstraints": ["Keep scope focused"],
                    "nextFocusHints": ["Continue with integration"],
                },
            }
        raise ValueError("model timeout")

    monkeypatch.setattr(plan_generator, "chat_completion_json", flaky_chat_completion_json)

    response = plan_generator.generate_plan_with_model(request)

    assert call_count == 4
    assert response.durationDays == 7
    assert len(response.days) == 7
    assert response.days[0].theme == "Theme 1"
    assert response.days[1].theme == "Theme 2"
    assert response.days[2].tasks

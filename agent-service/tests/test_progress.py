from fastapi.testclient import TestClient
from pytest import MonkeyPatch

from app.main import app
from app.schemas.progress import ProgressReviewRequest, ProgressReviewResponse
from app.services import progress_reviewer

client = TestClient(app)


def test_progress_review_returns_structured_stub_response(
    monkeypatch: MonkeyPatch,
) -> None:
    monkeypatch.setattr(progress_reviewer, "DEEPSEEK_API_KEY", "")

    response = client.post(
        "/agent/progress/review",
        json={
            "dayIndex": 1,
            "todayTasks": [
                {
                    "id": 1,
                    "title": "Create progress table",
                    "description": "Add migration and entity.",
                    "estimatedMinutes": 30,
                    "type": "build",
                    "deliverable": "progress_logs migration",
                    "priority": "high",
                },
                {
                    "id": 2,
                    "title": "Create progress form",
                    "description": "Build frontend submit form.",
                    "estimatedMinutes": 60,
                    "type": "build",
                    "deliverable": "Progress form",
                    "priority": "high",
                },
            ],
            "userFeedback": "Finished the API and still need UI polish.",
            "completedTasks": [
                {
                    "id": 1,
                    "title": "Create progress table",
                }
            ],
            "unfinishedTasks": [
                {
                    "id": 2,
                    "title": "Create progress form",
                }
            ],
            "blockers": ["Need server verification"],
        },
    )

    body = response.json()

    assert response.status_code == 200
    assert body["completedTasks"] == ["Create progress table"]
    assert body["unfinishedTasks"] == ["Create progress form"]
    assert body["blockers"] == ["Need server verification"]
    assert body["impact"] in {"none", "minor", "medium", "major"}
    assert body["suggestion"]


def test_progress_review_rejects_missing_tasks() -> None:
    response = client.post(
        "/agent/progress/review",
        json={
            "dayIndex": 1,
            "todayTasks": [],
            "userFeedback": "Nothing to review.",
        },
    )

    assert response.status_code == 422


def test_progress_model_output_is_normalized() -> None:
    request = ProgressReviewRequest(
        dayIndex=1,
        todayTasks=[
            {
                "id": 1,
                "title": "Create progress table",
            }
        ],
        userFeedback="Finished the table.",
    )
    parsed = {
        "progress_review": {
            "completed_tasks": [{"title": "Create progress table"}],
            "unfinished_tasks": [],
            "issues": "No blocker",
            "risk": "low",
            "advice": "Continue with the UI tomorrow.",
        }
    }

    normalized = progress_reviewer._normalize_model_output(parsed, request)
    response = ProgressReviewResponse.model_validate(normalized)

    assert response.completedTasks == ["Create progress table"]
    assert response.unfinishedTasks == []
    assert response.blockers == ["No blocker"]
    assert response.impact == "minor"
    assert response.suggestion == "Continue with the UI tomorrow."


def test_progress_model_failure_uses_mock_fallback(monkeypatch: MonkeyPatch) -> None:
    request = ProgressReviewRequest(
        dayIndex=1,
        todayTasks=[{"id": 1, "title": "Create progress table"}],
        userFeedback="Finished the table.",
        completedTasks=[{"id": 1, "title": "Create progress table"}],
    )

    def raise_invalid_response(
        _request: ProgressReviewRequest,
    ) -> ProgressReviewResponse:
        raise ValueError("invalid model response")

    monkeypatch.setattr(progress_reviewer, "DEEPSEEK_API_KEY", "test-key")
    monkeypatch.setattr(
        progress_reviewer,
        "review_progress_with_model",
        raise_invalid_response,
    )

    response = progress_reviewer.review_progress(request)

    assert response.completedTasks == ["Create progress table"]
    assert response.impact == "none"
    assert response.suggestion

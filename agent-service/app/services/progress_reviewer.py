import json
import re

from app.config import DEEPSEEK_API_KEY, PROFILE_ANALYZER_MODEL, PROGRESS_REVIEWER_TIMEOUT_SECONDS
from app.schemas.progress import ProgressReviewRequest, ProgressReviewResponse
from app.services.agent_execution import AgentExecutionResult, AgentResponseSource
from app.services.deepseek_chat import chat_completion_json
from app.services.language import is_zh
from app.services.model_retry import (
    ModelCallNonRetryableError,
    ModelCallRetryExhaustedError,
    retry_model_call,
)
from app.services.prompt_catalog import prompt_section


class ProgressReviewerError(RuntimeError):
    pass


def review_progress(
    request: ProgressReviewRequest,
) -> AgentExecutionResult[ProgressReviewResponse]:
    if DEEPSEEK_API_KEY:
        try:
            return AgentExecutionResult(
                payload=retry_model_call(lambda: review_progress_with_model(request)),
                source=AgentResponseSource.MODEL,
            )
        except ModelCallRetryExhaustedError:
            return AgentExecutionResult(
                payload=review_progress_with_mock(request),
                source=AgentResponseSource.FALLBACK,
            )
        except ModelCallNonRetryableError as exc:
            raise ProgressReviewerError(f"Progress reviewer model call failed: {exc}") from exc

    return AgentExecutionResult(
        payload=review_progress_with_mock(request),
        source=AgentResponseSource.FALLBACK,
    )


def review_progress_with_model(request: ProgressReviewRequest) -> ProgressReviewResponse:
    parsed = chat_completion_json(
        model=PROFILE_ANALYZER_MODEL,
        messages=[
            {
                "role": "system",
                "content": prompt_section(
                    "progress_reviewer",
                    "system_rules",
                    request.responseLanguage,
                ),
            },
            {
                "role": "user",
                "content": json.dumps(
                    {
                        "dayIndex": request.dayIndex,
                        "todayTasks": [
                            item.model_dump() for item in request.todayTasks
                        ],
                        "userFeedback": request.userFeedback,
                        "completedTasks": [
                            item.model_dump() for item in request.completedTasks
                        ],
                        "unfinishedTasks": [
                            item.model_dump() for item in request.unfinishedTasks
                        ],
                        "blockers": request.blockers,
                        "responseLanguage": request.responseLanguage,
                    },
                    ensure_ascii=False,
                ),
            },
        ],
        timeout_seconds=PROGRESS_REVIEWER_TIMEOUT_SECONDS,
        temperature=0.2,
        max_tokens=1000,
    )
    normalized = _normalize_model_output(parsed, request)
    return ProgressReviewResponse.model_validate(normalized)


def review_progress_with_mock(request: ProgressReviewRequest) -> ProgressReviewResponse:
    completed = [task.title for task in request.completedTasks]
    unfinished = [task.title for task in request.unfinishedTasks]
    blockers = _clean_blockers(request.blockers)
    impact = _impact_for_counts(
        total_count=len(request.todayTasks),
        completed_count=len(completed),
        unfinished_count=len(unfinished),
        blocker_count=len(blockers),
    )

    if impact == "none":
        suggestion = (
            "今天进展顺利。明天继续聚焦下一个学习重点。"
            if is_zh(request.responseLanguage)
            else "Good progress today. Keep tomorrow focused on the next learning priority."
        )
    elif impact == "minor":
        suggestion = (
            "把未完成项放到明天的开头，然后继续最高优先级的学习任务。"
            if is_zh(request.responseLanguage)
            else (
                "Carry the unfinished item into the start of tomorrow, then continue with the "
                "highest-priority planned task."
            )
        )
    elif impact == "medium":
        suggestion = (
            "明天先解除阻塞或完成未完成内容，再进入新的学习内容。"
            if is_zh(request.responseLanguage)
            else (
                "Start tomorrow by unblocking or finishing the incomplete work before moving "
                "into new content."
            )
        )
    else:
        suggestion = (
            "缩小明天范围，先解决主要阻塞，只保留一个高优先级重点任务。"
            if is_zh(request.responseLanguage)
            else (
                "Reduce tomorrow's scope, resolve the main blocker first, and keep only one "
                "high-priority focus task."
            )
        )

    wins = completed[:2]
    next_focus = unfinished[:2] if unfinished else blockers[:2]
    pace_adjustment = "keep"
    confidence = "medium"
    if impact == "none":
        pace_adjustment = "faster"
        confidence = "high"
    elif impact == "minor":
        pace_adjustment = "keep"
        confidence = "medium"
    elif impact == "medium":
        pace_adjustment = "slower"
        confidence = "medium"
    else:
        pace_adjustment = "slower"
        confidence = "low"

    if not wins and is_zh(request.responseLanguage):
        wins = ["今天至少完成了一部分计划内容。"]
    elif not wins:
        wins = ["At least part of the planned work was completed today."]

    if not next_focus and is_zh(request.responseLanguage):
        next_focus = ["先处理主要阻塞，再进入新的学习内容。"]
    elif not next_focus:
        next_focus = ["Clear the main blocker before moving into new work."]

    return ProgressReviewResponse(
        completedTasks=completed,
        unfinishedTasks=unfinished,
        blockers=blockers,
        impact=impact,
        suggestion=suggestion,
        wins=wins,
        nextFocus=next_focus,
        paceAdjustment=pace_adjustment,
        confidence=confidence,
    )


def _load_json_object(content: str) -> dict[str, object]:
    try:
        parsed = json.loads(content)
    except json.JSONDecodeError:
        match = re.search(r"\{.*\}", content, re.DOTALL)
        if not match:
            raise
        parsed = json.loads(match.group(0))

    if not isinstance(parsed, dict):
        raise ValueError("Progress reviewer response must be a JSON object.")

    return parsed


def _normalize_model_output(
    parsed: dict[str, object],
    request: ProgressReviewRequest,
) -> dict[str, object]:
    nested = _find_review_object(parsed)
    fallback = review_progress_with_mock(request)
    completed_tasks = _string_list(nested, "completedTasks", "completed_tasks")
    unfinished_tasks = _string_list(
        nested,
        "unfinishedTasks",
        "unfinished_tasks",
        "incompleteTasks",
        "incomplete_tasks",
    )
    blockers = _clean_blockers(
        _string_list(nested, "blockers", "blockingIssues", "issues")
        or request.blockers
    )
    impact_value = _first_string(nested, "impact", "risk", "level")
    pace_adjustment = _normalize_pace_adjustment(
        _first_string(nested, "paceAdjustment", "pace_adjustment", "pace")
    )
    confidence = _normalize_confidence(
        _first_string(nested, "confidence", "confidenceLevel", "confidence_level")
    )
    return {
        "completedTasks": completed_tasks or [task.title for task in request.completedTasks],
        "unfinishedTasks": unfinished_tasks or [task.title for task in request.unfinishedTasks],
        "blockers": blockers,
        "impact": _normalize_impact(impact_value)
        if impact_value.strip()
        else fallback.impact,
        "suggestion": _first_string(nested, "suggestion", "advice", "nextStep")
        or fallback.suggestion,
        "wins": _string_list(nested, "wins", "highlights", "achievements") or fallback.wins,
        "nextFocus": _string_list(nested, "nextFocus", "next_focus", "focus") or fallback.nextFocus,
        "paceAdjustment": pace_adjustment or "keep",
        "confidence": confidence or "medium",
    }


def _find_review_object(parsed: dict[str, object]) -> dict[object, object]:
    for key in ("review", "progressReview", "progress_review", "result", "data"):
        value = parsed.get(key)
        if isinstance(value, dict):
            return value
    return parsed


def _string_list(values: dict[object, object], *keys: str) -> list[str]:
    for key in keys:
        value = values.get(key)
        if isinstance(value, list):
            cleaned = []
            for item in value:
                if isinstance(item, str) and item.strip():
                    cleaned.append(item.strip())
                elif isinstance(item, dict):
                    title = _first_string(item, "title", "name", "task", "summary")
                    if title:
                        cleaned.append(title)
            return cleaned
        if isinstance(value, str) and value.strip():
            return [value.strip()]
    return []


def _first_string(values: dict[object, object], *keys: str) -> str:
    for key in keys:
        value = values.get(key)
        if isinstance(value, str) and value.strip():
            return value.strip()
        if isinstance(value, int | float):
            return str(value)
    return ""


def _normalize_impact(value: str) -> str:
    normalized = value.strip().lower()
    if normalized in {"none", "no", "zero", "0", "无"}:
        return "none"
    if normalized in {"minor", "low", "small", "slight", "轻微"}:
        return "minor"
    if normalized in {"medium", "moderate", "中等"}:
        return "medium"
    if normalized in {"major", "high", "severe", "blocked", "critical", "严重"}:
        return "major"
    return "medium"


def _normalize_pace_adjustment(value: str) -> str:
    normalized = value.strip().lower()
    if normalized in {"faster", "speed_up", "speedup", "加快", "更快"}:
        return "faster"
    if normalized in {"slower", "slow_down", "slowdown", "放慢", "更慢"}:
        return "slower"
    if normalized in {"keep", "steady", "maintain", "保持"}:
        return "keep"
    return ""


def _normalize_confidence(value: str) -> str:
    normalized = value.strip().lower()
    if normalized in {"low", "弱", "低"}:
        return "low"
    if normalized in {"high", "strong", "高"}:
        return "high"
    if normalized in {"medium", "moderate", "中", "中等"}:
        return "medium"
    return ""


def _clean_blockers(values: list[str]) -> list[str]:
    empty_markers = {"none", "no blocker", "no blockers", "无", "没有", "无阻塞"}
    cleaned: list[str] = []
    for value in values:
        normalized = value.strip()
        if not normalized:
            continue
        if normalized.lower() in empty_markers or normalized in empty_markers:
            continue
        cleaned.append(normalized)
    return cleaned


def _impact_for_counts(
    total_count: int,
    completed_count: int,
    unfinished_count: int,
    blocker_count: int,
) -> str:
    if blocker_count >= 2 or unfinished_count >= max(2, total_count):
        return "major"
    if blocker_count == 1 or unfinished_count > completed_count:
        return "medium"
    if unfinished_count > 0:
        return "minor"
    return "none"

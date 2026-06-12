import json
import re

import httpx

from app.config import (
    DEEPSEEK_API_BASE_URL,
    DEEPSEEK_API_KEY,
    PROFILE_ANALYZER_MODEL,
    PROFILE_ANALYZER_TIMEOUT_SECONDS,
)
from app.schemas.progress import ProgressReviewRequest, ProgressReviewResponse
from app.services.agent_execution import AgentExecutionResult, AgentResponseSource
from app.services.language import is_zh, prompt_for
from app.services.model_retry import (
    ModelCallNonRetryableError,
    ModelCallRetryExhaustedError,
    retry_model_call,
)

PROGRESS_REVIEWER_PROMPT_EN = """
You are the Progress Reviewer for AI Developer Learning Planner.

Review one day of learning progress. Return JSON only, with this exact shape:
{
  "completedTasks": ["string"],
  "unfinishedTasks": ["string"],
  "blockers": ["string"],
  "impact": "none|minor|medium|major",
  "suggestion": "string"
}

Rules:
- Do not include markdown fences or explanatory prose.
- completedTasks and unfinishedTasks should be concise task titles or summaries.
- blockers should include only concrete blockers from the user or task state.
- impact must be exactly one of none, minor, medium, major.
- Use major only when most tasks are unfinished or blockers prevent progress.
- Keep suggestion actionable for tomorrow, but do not rewrite the plan.
- Do not perform emotion analysis or complex learner assessment.
""".strip()

PROGRESS_REVIEWER_PROMPT_ZH = """
你是 AI Developer Learning Planner 的进度复盘器。

复盘某一天的学习进度。只返回 JSON，结构必须完全如下：
{
  "completedTasks": ["string"],
  "unfinishedTasks": ["string"],
  "blockers": ["string"],
  "impact": "none|minor|medium|major",
  "suggestion": "string"
}

规则：
- 不要包含 markdown 代码块或解释性正文。
- completedTasks、unfinishedTasks、blockers 和 suggestion 必须使用简体中文。
- completedTasks 和 unfinishedTasks 应是简洁任务标题或摘要。
- blockers 只包含用户或任务状态中的具体阻塞。
- impact 必须是 none、minor、medium、major 之一。
- 只有大多数任务未完成或阻塞阻止进度时才使用 major。
- suggestion 要对明天可执行，但不要重写计划。
- 不要做情绪分析或复杂学习者评估。
""".strip()


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
    response = httpx.post(
        f"{DEEPSEEK_API_BASE_URL.rstrip('/')}/chat/completions",
        headers={
            "Authorization": f"Bearer {DEEPSEEK_API_KEY}",
            "Content-Type": "application/json",
        },
        json={
            "model": PROFILE_ANALYZER_MODEL,
            "messages": [
                {
                    "role": "system",
                    "content": prompt_for(
                        request.responseLanguage,
                        PROGRESS_REVIEWER_PROMPT_ZH,
                        PROGRESS_REVIEWER_PROMPT_EN,
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
            "temperature": 0.2,
        },
        timeout=PROFILE_ANALYZER_TIMEOUT_SECONDS,
    )
    response.raise_for_status()

    body = response.json()
    content = body["choices"][0]["message"]["content"]
    parsed = _load_json_object(content)
    normalized = _normalize_model_output(parsed, request)
    return ProgressReviewResponse.model_validate(normalized)


def review_progress_with_mock(request: ProgressReviewRequest) -> ProgressReviewResponse:
    completed = [task.title for task in request.completedTasks]
    unfinished = [task.title for task in request.unfinishedTasks]
    impact = _impact_for_counts(
        total_count=len(request.todayTasks),
        completed_count=len(completed),
        unfinished_count=len(unfinished),
        blocker_count=len(request.blockers),
    )

    if impact == "none":
        suggestion = (
            "今天进展顺利。明天继续聚焦下一个计划切片。"
            if is_zh(request.responseLanguage)
            else "Good progress today. Keep tomorrow focused on the next planned slice."
        )
    elif impact == "minor":
        suggestion = (
            "把未完成项放到明天热身环节，然后继续最高优先级计划任务。"
            if is_zh(request.responseLanguage)
            else (
                "Carry the unfinished item into tomorrow's warm-up, then continue with the "
                "highest-priority planned task."
            )
        )
    elif impact == "medium":
        suggestion = (
            "明天先解除阻塞或完成未完成工作，再增加新的范围。"
            if is_zh(request.responseLanguage)
            else (
                "Start tomorrow by unblocking or finishing the incomplete work before adding "
                "new scope."
            )
        )
    else:
        suggestion = (
            "缩小明天范围，先解决主要阻塞，只保留一个高优先级实现任务。"
            if is_zh(request.responseLanguage)
            else (
                "Reduce tomorrow's scope, resolve the main blocker first, and keep only one "
                "high-priority implementation task."
            )
        )

    return ProgressReviewResponse(
        completedTasks=completed,
        unfinishedTasks=unfinished,
        blockers=request.blockers,
        impact=impact,
        suggestion=suggestion,
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
    return {
        "completedTasks": _string_list(nested, "completedTasks", "completed_tasks"),
        "unfinishedTasks": _string_list(
            nested,
            "unfinishedTasks",
            "unfinished_tasks",
            "incompleteTasks",
            "incomplete_tasks",
        ),
        "blockers": _string_list(nested, "blockers", "blockingIssues", "issues")
        or request.blockers,
        "impact": _normalize_impact(_first_string(nested, "impact", "risk", "level")),
        "suggestion": _first_string(nested, "suggestion", "advice", "nextStep")
        or review_progress_with_mock(request).suggestion,
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
    if normalized in {"none", "no", "zero", "0"}:
        return "none"
    if normalized in {"minor", "low", "small", "slight"}:
        return "minor"
    if normalized in {"major", "high", "severe", "blocked", "critical"}:
        return "major"
    return "medium"


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

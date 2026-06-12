import json
import re

from app.config import DEEPSEEK_API_KEY, PLAN_ADJUSTER_TIMEOUT_SECONDS, PROFILE_ANALYZER_MODEL
from app.schemas.plan import (
    PlanAdjustRequest,
    PlanAdjustResponse,
    PlanAdjustTask,
    PlanMovedTask,
    PlanSplitTask,
)
from app.services.agent_execution import AgentExecutionResult, AgentResponseSource
from app.services.deepseek_chat import chat_completion_json
from app.services.language import is_zh
from app.services.model_retry import (
    ModelCallNonRetryableError,
    ModelCallRetryExhaustedError,
    retry_model_call,
)
from app.services.prompt_catalog import prompt_section


class PlanAdjusterError(RuntimeError):
    pass


def adjust_plan(
    request: PlanAdjustRequest,
) -> AgentExecutionResult[PlanAdjustResponse]:
    if DEEPSEEK_API_KEY:
        try:
            return AgentExecutionResult(
                payload=retry_model_call(lambda: adjust_plan_with_model(request)),
                source=AgentResponseSource.MODEL,
            )
        except ModelCallRetryExhaustedError:
            return AgentExecutionResult(
                payload=adjust_plan_with_mock(request),
                source=AgentResponseSource.FALLBACK,
            )
        except ModelCallNonRetryableError as exc:
            raise PlanAdjusterError(f"Plan adjuster model call failed: {exc}") from exc

    return AgentExecutionResult(
        payload=adjust_plan_with_mock(request),
        source=AgentResponseSource.FALLBACK,
    )


def adjust_plan_with_model(request: PlanAdjustRequest) -> PlanAdjustResponse:
    parsed = chat_completion_json(
        model=PROFILE_ANALYZER_MODEL,
        messages=[
            {
                "role": "system",
                "content": prompt_section(
                    "plan_adjuster",
                    "system_rules",
                    request.responseLanguage,
                ),
            },
            {
                "role": "user",
                "content": json.dumps(request.model_dump(), ensure_ascii=False),
            },
        ],
        timeout_seconds=PLAN_ADJUSTER_TIMEOUT_SECONDS,
        temperature=0.2,
        max_tokens=1600,
    )
    normalized = _normalize_model_output(parsed, request)
    return PlanAdjustResponse.model_validate(normalized)


def adjust_plan_with_mock(request: PlanAdjustRequest) -> PlanAdjustResponse:
    target_day = _target_day_index(request)
    next_day_tasks = list(request.nextDayTasks)
    moved_tasks: list[PlanMovedTask] = []
    split_tasks: list[PlanSplitTask] = []
    carried_tasks: list[PlanAdjustTask] = []

    for task in request.unfinishedTasks:
        if _should_split(task, request.progressReview.impact):
            part_minutes = max(15, task.estimatedMinutes // 2)
            parts = [
                _copy_task(
                    task,
                    title=(
                        f"{task.title} - 第 1 部分"
                        if is_zh(request.responseLanguage)
                        else f"{task.title} - part 1"
                    ),
                    description=(
                        f"完成该任务的第一个聚焦部分：{task.description}"
                        if is_zh(request.responseLanguage)
                        else f"Complete the first focused part of: {task.description}"
                    ),
                    estimated_minutes=part_minutes,
                    day_index=target_day,
                ),
                _copy_task(
                    task,
                    title=(
                        f"{task.title} - 第 2 部分"
                        if is_zh(request.responseLanguage)
                        else f"{task.title} - part 2"
                    ),
                    description=(
                        f"完成该任务的剩余部分：{task.description}"
                        if is_zh(request.responseLanguage)
                        else f"Complete the remaining part of: {task.description}"
                    ),
                    estimated_minutes=max(15, task.estimatedMinutes - part_minutes),
                    day_index=target_day,
                ),
            ]
            split_tasks.append(
                PlanSplitTask(
                    sourceTaskId=task.id,
                    sourceTitle=task.title,
                    parts=parts,
                    reason=(
                        "该未完成任务较大，适合拆成下一天可聚焦完成的小部分。"
                        if is_zh(request.responseLanguage)
                        else (
                            "The unfinished task is large enough to split into "
                            "focused next-day parts."
                        )
                    ),
                )
            )
            carried_tasks.extend(parts)
        else:
            carried = _copy_task(
                task,
                title=(
                    f"结转：{task.title}"
                    if is_zh(request.responseLanguage)
                    else f"Carry over: {task.title}"
                ),
                description=task.description,
                estimated_minutes=task.estimatedMinutes,
                day_index=target_day,
            )
            carried_tasks.append(carried)
            moved_tasks.append(
                PlanMovedTask(
                    taskId=task.id,
                    title=task.title,
                    fromDayIndex=request.currentDayIndex,
                    toDayIndex=target_day,
                    reason=(
                        "该任务尚未完成，应在新增范围前优先处理。"
                        if is_zh(request.responseLanguage)
                        else "The task was unfinished and should be handled before new scope."
                    ),
                )
            )

    reason = _reason_for(request, carried_tasks)
    return PlanAdjustResponse(
        nextDayTasks=_dedupe_tasks([*carried_tasks, *next_day_tasks], request),
        movedTasks=moved_tasks,
        splitTasks=split_tasks,
        reason=reason,
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
        raise ValueError("Plan adjuster response must be a JSON object.")

    return parsed


def _normalize_model_output(
    parsed: dict[str, object],
    request: PlanAdjustRequest,
) -> dict[str, object]:
    nested = _find_adjustment_object(parsed)
    fallback = adjust_plan_with_mock(request)
    next_day_tasks = _dedupe_tasks(
        _task_list(nested, request, "nextDayTasks", "next_day_tasks")
        or [task.model_dump() for task in fallback.nextDayTasks],
        request,
    )
    return {
        "nextDayTasks": next_day_tasks,
        "movedTasks": _moved_list(nested, request, "movedTasks", "moved_tasks")
        or [task.model_dump() for task in fallback.movedTasks],
        "splitTasks": _split_list(nested, request, "splitTasks", "split_tasks")
        or [task.model_dump() for task in fallback.splitTasks],
        "reason": _first_string(nested, "reason", "adjustmentReason", "summary")
        or fallback.reason,
    }


def _find_adjustment_object(parsed: dict[str, object]) -> dict[object, object]:
    for key in ("adjustment", "planAdjustment", "plan_adjustment", "result", "data"):
        value = parsed.get(key)
        if isinstance(value, dict):
            return value
    return parsed


def _task_list(
    values: dict[object, object],
    request: PlanAdjustRequest,
    *keys: str,
) -> list[dict[str, object]]:
    for key in keys:
        value = values.get(key)
        if isinstance(value, list):
            return [
                _normalize_task(item, request)
                for item in value
                if isinstance(item, dict)
            ]
    return []


def _moved_list(
    values: dict[object, object],
    request: PlanAdjustRequest,
    *keys: str,
) -> list[dict[str, object]]:
    target_day = _target_day_index(request)
    moved = []
    for key in keys:
        value = values.get(key)
        if isinstance(value, list):
            for item in value:
                if isinstance(item, dict):
                    title = _first_string(item, "title", "taskTitle", "name")
                    moved.append(
                        {
                            "taskId": _first_int(item, "taskId", "id", "sourceTaskId"),
                            "title": title
                            or (
                                "未完成任务"
                                if is_zh(request.responseLanguage)
                                else "Unfinished task"
                            ),
                            "fromDayIndex": _first_int(item, "fromDayIndex")
                            or request.currentDayIndex,
                            "toDayIndex": target_day,
                            "reason": _first_string(item, "reason")
                            or (
                                "该任务尚未完成。"
                                if is_zh(request.responseLanguage)
                                else "The task was unfinished."
                            ),
                        }
                    )
            return moved
    return moved


def _split_list(
    values: dict[object, object],
    request: PlanAdjustRequest,
    *keys: str,
) -> list[dict[str, object]]:
    split = []
    for key in keys:
        value = values.get(key)
        if isinstance(value, list):
            for item in value:
                if isinstance(item, dict):
                    parts = item.get("parts")
                    if not isinstance(parts, list):
                        parts = []
                    split.append(
                        {
                            "sourceTaskId": _first_int(
                                item,
                                "sourceTaskId",
                                "taskId",
                                "id",
                            ),
                            "sourceTitle": _first_string(
                                item,
                                "sourceTitle",
                                "title",
                                "taskTitle",
                            )
                            or ("拆分任务" if is_zh(request.responseLanguage) else "Split task"),
                            "parts": [
                                _normalize_task(part, request)
                                for part in parts
                                if isinstance(part, dict)
                            ],
                            "reason": _first_string(item, "reason")
                            or (
                                "该任务被拆分以降低下一天负担。"
                                if is_zh(request.responseLanguage)
                                else "The task was split to reduce the next-day load."
                            ),
                        }
                    )
            return split
    return split


def _normalize_task(
    item: dict[object, object],
    request: PlanAdjustRequest,
) -> dict[str, object]:
    target_day = _target_day_index(request)
    return {
        "id": _first_int(item, "id", "taskId"),
        "dayIndex": target_day,
        "taskOrder": _first_int(item, "taskOrder", "order"),
        "title": _first_string(item, "title", "name", "task")
        or ("调整后的学习任务" if is_zh(request.responseLanguage) else "Adjusted learning task"),
        "description": _first_string(item, "description", "details", "summary")
        or (
            "完成调整后的学习任务。"
            if is_zh(request.responseLanguage)
            else "Complete the adjusted learning task."
        ),
        "estimatedMinutes": _first_int(item, "estimatedMinutes", "minutes", "duration")
        or 45,
        "type": _first_string(item, "type", "category") or "practice",
        "deliverable": _first_string(item, "deliverable", "output")
        or (
            "更新后的学习产物"
            if is_zh(request.responseLanguage)
            else "Updated learning artifact"
        ),
        "priority": _normalize_priority(_first_string(item, "priority", "urgency")),
        "status": "PENDING",
    }


def _copy_task(
    task: PlanAdjustTask,
    title: str,
    description: str,
    estimated_minutes: int,
    day_index: int,
) -> PlanAdjustTask:
    return PlanAdjustTask(
        dayIndex=day_index,
        title=title,
        description=description,
        estimatedMinutes=estimated_minutes,
        type=task.type,
        deliverable=task.deliverable,
        priority=task.priority,
        status="PENDING",
    )


def _should_split(task: PlanAdjustTask, impact: str) -> bool:
    return task.estimatedMinutes >= 90 or (
        impact in {"medium", "major"} and task.estimatedMinutes >= 75
    )


def _target_day_index(request: PlanAdjustRequest) -> int:
    return request.currentDayIndex + 1


def _reason_for(request: PlanAdjustRequest, carried_tasks: list[PlanAdjustTask]) -> str:
    if not carried_tasks:
        return (
            "没有未完成任务，因此不需要本地调整计划。"
            if is_zh(request.responseLanguage)
            else "No local plan change is needed because there are no unfinished tasks."
        )
    if request.progressReview.impact in {"medium", "major"}:
        return (
            "明天计划已调整为先处理未完成工作和阻塞，再进入新范围。"
            if is_zh(request.responseLanguage)
            else (
                "Tomorrow's plan was adjusted to resolve unfinished work and "
                "blockers before new content."
            )
        )
    return (
        "明天计划已调整为先结转未完成工作，再进入新范围。"
        if is_zh(request.responseLanguage)
        else "Tomorrow's plan was adjusted to carry over unfinished work before new content."
    )


def _dedupe_tasks(
    tasks: list[PlanAdjustTask | dict[str, object]],
    request: PlanAdjustRequest,
) -> list[dict[str, object]]:
    deduped: list[dict[str, object]] = []
    seen: set[tuple[str, str]] = set()
    for item in tasks:
        normalized = item.model_dump() if isinstance(item, PlanAdjustTask) else _normalize_task(item, request)
        key = (
            str(normalized.get("title", "")).strip().lower(),
            str(normalized.get("deliverable", "")).strip().lower(),
        )
        if key in seen:
            continue
        seen.add(key)
        deduped.append(normalized)
    for index, item in enumerate(deduped, start=1):
        item["taskOrder"] = index
    return deduped


def _first_string(values: dict[object, object], *keys: str) -> str:
    for key in keys:
        value = values.get(key)
        if isinstance(value, str) and value.strip():
            return value.strip()
        if isinstance(value, int | float):
            return str(value)
    return ""


def _first_int(values: dict[object, object], *keys: str) -> int | None:
    for key in keys:
        value = values.get(key)
        if isinstance(value, int) and value > 0:
            return value
        if isinstance(value, str):
            match = re.search(r"\d+", value)
            if match:
                parsed = int(match.group(0))
                if parsed > 0:
                    return parsed
    return None


def _normalize_priority(value: str) -> str:
    normalized = value.strip().lower()
    if normalized in {"high", "urgent", "critical", "p0", "p1"}:
        return "high"
    if normalized in {"low", "optional", "p3"}:
        return "low"
    return "medium"

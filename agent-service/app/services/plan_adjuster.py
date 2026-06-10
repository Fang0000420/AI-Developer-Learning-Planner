import json
import re

import httpx
from pydantic import ValidationError

from app.config import (
    DEEPSEEK_API_BASE_URL,
    DEEPSEEK_API_KEY,
    PROFILE_ANALYZER_MODEL,
    PROFILE_ANALYZER_TIMEOUT_SECONDS,
)
from app.schemas.plan import (
    PlanAdjustRequest,
    PlanAdjustResponse,
    PlanAdjustTask,
    PlanMovedTask,
    PlanSplitTask,
)
from app.services.model_retry import retry_model_call

PLAN_ADJUSTER_PROMPT = """
You are the Plan Adjuster for AI Developer Learning Planner.

Adjust only the next day of an existing learning plan. Return JSON only, with this exact shape:
{
  "nextDayTasks": [
    {
      "id": 123,
      "dayIndex": 2,
      "taskOrder": 1,
      "title": "string",
      "description": "string",
      "estimatedMinutes": 45,
      "type": "build",
      "deliverable": "string",
      "priority": "high|medium|low",
      "status": "PENDING"
    }
  ],
  "movedTasks": [
    {
      "taskId": 123,
      "title": "string",
      "fromDayIndex": 1,
      "toDayIndex": 2,
      "reason": "string"
    }
  ],
  "splitTasks": [
    {
      "sourceTaskId": 123,
      "sourceTitle": "string",
      "parts": [
        {
          "title": "string",
          "description": "string",
          "estimatedMinutes": 45,
          "type": "build",
          "deliverable": "string",
          "priority": "high"
        }
      ],
      "reason": "string"
    }
  ],
  "reason": "string"
}

Rules:
- Do not include markdown fences or explanatory prose.
- Do not repeat completed tasks.
- Prioritize unfinished tasks before adding new scope.
- Split tasks that are too large for a focused next-day session.
- Adjust only the next day; do not rewrite the whole plan.
- Preserve existing next-day tasks unless the unfinished work needs the first slot.
""".strip()


class PlanAdjusterError(RuntimeError):
    pass


def adjust_plan(request: PlanAdjustRequest) -> PlanAdjustResponse:
    if DEEPSEEK_API_KEY:
        try:
            return retry_model_call(lambda: adjust_plan_with_model(request))
        except (httpx.HTTPError, KeyError, TypeError, ValueError, ValidationError):
            return adjust_plan_with_mock(request)

    return adjust_plan_with_mock(request)


def adjust_plan_with_model(request: PlanAdjustRequest) -> PlanAdjustResponse:
    response = httpx.post(
        f"{DEEPSEEK_API_BASE_URL.rstrip('/')}/chat/completions",
        headers={
            "Authorization": f"Bearer {DEEPSEEK_API_KEY}",
            "Content-Type": "application/json",
        },
        json={
            "model": PROFILE_ANALYZER_MODEL,
            "messages": [
                {"role": "system", "content": PLAN_ADJUSTER_PROMPT},
                {
                    "role": "user",
                    "content": json.dumps(request.model_dump(), ensure_ascii=False),
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
                    title=f"{task.title} - part 1",
                    description=f"Complete the first focused slice of: {task.description}",
                    estimated_minutes=part_minutes,
                    day_index=target_day,
                ),
                _copy_task(
                    task,
                    title=f"{task.title} - part 2",
                    description=f"Complete the remaining focused slice of: {task.description}",
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
                        "The unfinished task is large enough to split into "
                        "focused next-day slices."
                    ),
                )
            )
            carried_tasks.extend(parts)
        else:
            carried = _copy_task(
                task,
                title=f"Carry over: {task.title}",
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
                    reason="The task was unfinished and should be handled before new scope.",
                )
            )

    reason = _reason_for(request, carried_tasks)
    return PlanAdjustResponse(
        nextDayTasks=[*carried_tasks, *next_day_tasks],
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
    return {
        "nextDayTasks": _task_list(nested, request, "nextDayTasks", "next_day_tasks")
        or [task.model_dump() for task in fallback.nextDayTasks],
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
                            "title": title or "Unfinished task",
                            "fromDayIndex": _first_int(item, "fromDayIndex")
                            or request.currentDayIndex,
                            "toDayIndex": _first_int(item, "toDayIndex") or target_day,
                            "reason": _first_string(item, "reason")
                            or "The task was unfinished.",
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
                            or "Split task",
                            "parts": [
                                _normalize_task(part, request)
                                for part in parts
                                if isinstance(part, dict)
                            ],
                            "reason": _first_string(item, "reason")
                            or "The task was split to reduce next-day scope.",
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
        "dayIndex": _first_int(item, "dayIndex", "day") or target_day,
        "taskOrder": _first_int(item, "taskOrder", "order"),
        "title": _first_string(item, "title", "name", "task")
        or "Adjusted learning task",
        "description": _first_string(item, "description", "details", "summary")
        or "Complete the adjusted learning task.",
        "estimatedMinutes": _first_int(item, "estimatedMinutes", "minutes", "duration")
        or 45,
        "type": _first_string(item, "type", "category") or "build",
        "deliverable": _first_string(item, "deliverable", "output")
        or "Updated learning artifact",
        "priority": _normalize_priority(_first_string(item, "priority", "urgency")),
        "status": _first_string(item, "status") or "PENDING",
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
        return "No local plan change is needed because there are no unfinished tasks."
    if request.progressReview.impact in {"medium", "major"}:
        return (
            "Tomorrow's plan was adjusted to resolve unfinished work and "
            "blockers before new scope."
        )
    return "Tomorrow's plan was adjusted to carry over unfinished work before new scope."


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

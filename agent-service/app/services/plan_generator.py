import json
import math
import re

from app.config import (
    DEEPSEEK_API_KEY,
    PLAN_GENERATOR_MODEL,
    PLAN_GENERATOR_TIMEOUT_SECONDS,
)
from app.schemas.plan import (
    PlanDay,
    PlanGenerateChunkResponse,
    PlanGenerateRequest,
    PlanGenerateResponse,
    PlanGenerationMemory,
    PlanTask,
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


class PlanGeneratorError(RuntimeError):
    pass


def generate_plan(
    request: PlanGenerateRequest,
) -> AgentExecutionResult[PlanGenerateResponse]:
    if DEEPSEEK_API_KEY:
        try:
            return AgentExecutionResult(
                payload=retry_model_call(lambda: generate_plan_with_model(request)),
                source=AgentResponseSource.MODEL,
            )
        except ModelCallRetryExhaustedError:
            return AgentExecutionResult(
                payload=generate_plan_with_mock(request),
                source=AgentResponseSource.FALLBACK,
            )
        except ModelCallNonRetryableError as exc:
            raise PlanGeneratorError(f"Plan generator model call failed: {exc}") from exc

    return AgentExecutionResult(
        payload=generate_plan_with_mock(request),
        source=AgentResponseSource.FALLBACK,
    )


def generate_plan_with_model(request: PlanGenerateRequest) -> PlanGenerateResponse:
    days: list[dict[str, object]] = []
    memory = PlanGenerationMemory()
    plan_title = ""
    previous_chunk: dict[str, object] | None = None
    chunk_size = 2
    daily_limit = _daily_minutes_limit(request)

    for start_day in range(1, request.durationDays + 1, chunk_size):
        end_day = min(request.durationDays, start_day + chunk_size - 1)
        try:
            chunk_response = retry_model_call(
                lambda start_day=start_day, end_day=end_day, previous_chunk=previous_chunk, memory=memory:
                _generate_plan_chunk(
                    request,
                    start_day,
                    end_day,
                    previous_chunk,
                    memory,
                )
            )
        except ModelCallRetryExhaustedError:
            while len(days) < request.durationDays:
                days.append(
                    _build_mock_day(len(days) + 1, request, daily_limit).model_dump()
                )
            break

        normalized_chunk = _normalize_plan_chunk(chunk_response, request, start_day, end_day)
        if not plan_title:
            plan_title = normalized_chunk.planTitle
        days.extend(day.model_dump() for day in normalized_chunk.days)
        memory = normalized_chunk.memory
        previous_chunk = normalized_chunk.model_dump()

    while len(days) < request.durationDays:
        days.append(_build_mock_day(len(days) + 1, request, daily_limit).model_dump())

    response = {
        "planTitle": plan_title
        or _default_plan_title(request),
        "durationDays": request.durationDays,
        "days": days[: request.durationDays],
    }
    return PlanGenerateResponse.model_validate(response)


def _generate_plan_chunk(
    request: PlanGenerateRequest,
    start_day: int,
    end_day: int,
    previous_chunk: dict[str, object] | None,
    memory: PlanGenerationMemory,
) -> dict[str, object]:
    parsed = chat_completion_json(
        model=PLAN_GENERATOR_MODEL,
        messages=_plan_generation_messages(
            request,
            start_day,
            end_day,
            previous_chunk,
            memory,
        ),
        timeout_seconds=PLAN_GENERATOR_TIMEOUT_SECONDS,
        temperature=0.2,
        max_tokens=2200,
    )
    return _load_json_object(json.dumps(parsed, ensure_ascii=False))


def _plan_generation_messages(
    request: PlanGenerateRequest,
    start_day: int,
    end_day: int,
    previous_chunk: dict[str, object] | None,
    memory: PlanGenerationMemory,
) -> list[dict[str, str]]:
    global_context = {
        "mainGoal": request.mainGoal,
        "currentSkills": request.currentSkills,
        "strengths": request.strengths,
        "weaknesses": request.weaknesses,
        "subGoals": [item.model_dump() for item in request.subGoals],
        "skillGaps": [item.model_dump() for item in request.skillGaps],
        "recommendedProject": request.recommendedProject,
        "projectReason": request.projectReason,
        "difficulty": request.difficulty,
        "coreTechStack": request.coreTechStack,
        "finalDeliverables": request.finalDeliverables,
        "durationDays": request.durationDays,
        "dailyAvailableHours": request.dailyAvailableHours,
        "responseLanguage": request.responseLanguage,
    }
    messages = [
        {
            "role": "system",
            "content": prompt_section("plan_generator", "system_rules", request.responseLanguage),
        },
        {
            "role": "user",
            "content": (
                prompt_section(
                    "plan_generator",
                    "global_context_instruction",
                    request.responseLanguage,
                )
                + "\n"
                + json.dumps(global_context, ensure_ascii=False)
            ),
        },
    ]
    if previous_chunk is not None:
        messages.append(
            {
                "role": "assistant",
                "content": json.dumps(previous_chunk, ensure_ascii=False),
            }
        )
    messages.append(
        {
            "role": "user",
            "content": (
                prompt_section(
                    "plan_generator",
                    "memory_instruction",
                    request.responseLanguage,
                )
                + "\n"
                + json.dumps(
                    {
                        "previousMemory": memory.model_dump(),
                        "previousChunk": previous_chunk,
                    },
                    ensure_ascii=False,
                )
                + "\n"
                + prompt_section(
                    "plan_generator",
                    "round_instruction",
                    request.responseLanguage,
                ).format(start_day=start_day, end_day=end_day)
            ),
        }
    )
    return messages


def _normalize_plan_chunk(
    parsed: dict[str, object],
    request: PlanGenerateRequest,
    start_day: int,
    end_day: int,
) -> PlanGenerateChunkResponse:
    nested = _find_plan_object(parsed)
    raw_days = _raw_days(nested)
    daily_limit = _daily_minutes_limit(request)
    expected_indexes = list(range(start_day, end_day + 1))
    normalized_days = [
        PlanDay.model_validate(
            _normalize_day(
                item,
                expected_indexes[index],
                request,
                daily_limit,
            )
        )
        for index, item in enumerate(raw_days[: len(expected_indexes)])
        if isinstance(item, dict)
    ]
    while len(normalized_days) < len(expected_indexes):
        normalized_days.append(
            _build_mock_day(expected_indexes[len(normalized_days)], request, daily_limit)
        )

    raw_memory = nested.get("memory")
    if not isinstance(raw_memory, dict):
        raw_memory = parsed.get("memory")
    memory = _normalize_plan_memory(raw_memory, expected_indexes, normalized_days)

    return PlanGenerateChunkResponse(
        planTitle=_first_string(
            nested,
            "planTitle",
            "plan_title",
            "title",
            "name",
        )
        or _default_plan_title(request),
        days=normalized_days,
        memory=memory,
    )


def _normalize_plan_memory(
    raw_memory: dict[object, object] | None,
    expected_indexes: list[int],
    days: list[PlanDay],
) -> PlanGenerationMemory:
    values = raw_memory if isinstance(raw_memory, dict) else {}
    return PlanGenerationMemory(
        completedDayIndexes=_int_list(values.get("completedDayIndexes")) or expected_indexes,
        establishedThemes=_string_list_from_value(values.get("establishedThemes"))
        or [day.theme for day in days],
        carryForwardConstraints=_string_list_from_value(values.get("carryForwardConstraints")),
        nextFocusHints=_string_list_from_value(values.get("nextFocusHints")),
    )


def generate_plan_with_mock(request: PlanGenerateRequest) -> PlanGenerateResponse:
    daily_limit = _daily_minutes_limit(request)
    days = [
        _build_mock_day(day_index, request, daily_limit)
        for day_index in range(1, request.durationDays + 1)
    ]
    return PlanGenerateResponse(
        planTitle=_default_plan_title(request),
        durationDays=request.durationDays,
        days=days,
    )


def _build_mock_day(
    day_index: int,
    request: PlanGenerateRequest,
    daily_limit: int,
) -> PlanDay:
    if is_zh(request.responseLanguage):
        phases = [
            ("认知建立", "明确目标要求、限制条件和当前基础。"),
            ("基础训练", "围绕关键能力做聚焦练习并补齐短板。"),
            ("场景应用", "把所学内容应用到接近真实场景的任务中。"),
            ("输出验证", "通过作品、测验、演示或记录验证进展。"),
            ("复盘强化", "整理反馈、修正方法并强化薄弱环节。"),
            ("巩固迁移", "巩固成果并迁移到更完整的应用场景。"),
        ]
    else:
        phases = [
            ("Orientation", "Clarify the goal, constraints, and current foundation."),
            ("Foundation", "Build the core knowledge and practice habits needed for progress."),
            ("Application", "Apply the learning in tasks that resemble real scenarios."),
            ("Validation", "Use outputs, checks, or demonstrations to verify progress."),
            ("Review", "Reflect on feedback and reinforce weak areas."),
            ("Transfer", "Consolidate gains and extend them into broader situations."),
        ]
    phase_index = min(
        len(phases) - 1,
        math.floor((day_index - 1) * len(phases) / request.durationDays),
    )
    phase, theme = phases[phase_index]
    focus = _focus_for_day(day_index, request)
    task_minutes = _task_minutes(daily_limit)

    if is_zh(request.responseLanguage):
        tasks = [
            PlanTask(
                title=f"梳理第 {day_index} 天的 {focus} 学习重点",
                description="先复盘目标、相关差距和预期成果，再明确今天最重要的学习任务。",
                estimatedMinutes=task_minutes[0],
                type="learn",
                deliverable=f"第 {day_index} 天学习重点说明",
                priority="high",
            ),
            PlanTask(
                title=f"练习并应用 {focus}",
                description=f"围绕今日主题完成一个可验证的小练习或实际应用任务，推进目标达成。",
                estimatedMinutes=task_minutes[1],
                type="practice",
                deliverable=f"{focus} 阶段成果",
                priority="high",
            ),
        ]
    else:
        tasks = [
            PlanTask(
                title=f"Map Day {day_index} learning focus for {focus}",
                description=(
                    "Review the goal, relevant gaps, and expected outcomes before deciding "
                    "what matters most today."
                ),
                estimatedMinutes=task_minutes[0],
                type="learn",
                deliverable=f"Day {day_index} focus notes",
                priority="high",
            ),
            PlanTask(
                title=f"Practice and apply {focus}",
                description=(
                    "Complete a small but verifiable exercise or applied task that moves "
                    "today's theme forward."
                ),
                estimatedMinutes=task_minutes[1],
                type="practice",
                deliverable=f"{focus} progress artifact",
                priority="high",
            ),
        ]

    if len(task_minutes) == 3:
        tasks.append(
            PlanTask(
                title=(
                    f"验证并记录第 {day_index} 天进展"
                    if is_zh(request.responseLanguage)
                    else f"Verify and record Day {day_index} progress"
                ),
                description=(
                    "回看今天的结果，记录有效做法、仍有缺口的地方，以及明天要继续的内容。"
                    if is_zh(request.responseLanguage)
                    else (
                        "Review today's results, capture what worked, note the remaining "
                        "gaps, and write down what should continue tomorrow."
                    )
                ),
                estimatedMinutes=task_minutes[2],
                type="review",
                deliverable=(
                    f"第 {day_index} 天验证记录"
                    if is_zh(request.responseLanguage)
                    else f"Day {day_index} verification note"
                ),
                priority="medium",
            )
        )

    return PlanDay(
        dayIndex=day_index,
        theme=f"{phase}: {theme}",
        tasks=tasks,
    )


def _focus_for_day(day_index: int, request: PlanGenerateRequest) -> str:
    if request.subGoals:
        return request.subGoals[(day_index - 1) % len(request.subGoals)].title
    if request.skillGaps:
        return request.skillGaps[(day_index - 1) % len(request.skillGaps)].skill
    if request.coreTechStack:
        return request.coreTechStack[(day_index - 1) % len(request.coreTechStack)]
    return "学习重点" if is_zh(request.responseLanguage) else "learning focus"


def _default_plan_title(request: PlanGenerateRequest) -> str:
    track_title = request.recommendedProject or request.mainGoal
    if is_zh(request.responseLanguage):
        return f"{request.durationDays} 天 {track_title} 学习计划"
    return f"{request.durationDays}-Day {track_title} Learning Plan"


def _task_minutes(daily_limit: int) -> list[int]:
    if daily_limit < 90:
        first = max(20, daily_limit // 3)
        second = max(30, daily_limit - first)
        return [first, second]

    first = max(30, daily_limit // 4)
    second = max(45, daily_limit // 2)
    third = max(20, daily_limit - first - second)
    return [first, second, third]


def _load_json_object(content: str) -> dict[str, object]:
    try:
        parsed = json.loads(content)
    except json.JSONDecodeError:
        match = re.search(r"\{.*\}", content, re.DOTALL)
        if not match:
            raise
        parsed = json.loads(match.group(0))

    if not isinstance(parsed, dict):
        raise ValueError("Plan generator response must be a JSON object.")

    return parsed


def _normalize_model_output(
    parsed: dict[str, object],
    request: PlanGenerateRequest,
) -> dict[str, object]:
    nested = _find_plan_object(parsed)
    daily_limit = _daily_minutes_limit(request)
    raw_days = _raw_days(nested)
    normalized_days = [
        _normalize_day(item, index + 1, request, daily_limit)
        for index, item in enumerate(raw_days[: request.durationDays])
        if isinstance(item, dict)
    ]

    while len(normalized_days) < request.durationDays:
        normalized_days.append(
            _build_mock_day(len(normalized_days) + 1, request, daily_limit).model_dump()
        )

    return {
        "planTitle": _first_string(
            nested,
            "planTitle",
            "plan_title",
            "title",
            "name",
        )
        or _default_plan_title(request),
        "durationDays": request.durationDays,
        "days": normalized_days,
    }


def _normalize_day(
    value: dict[object, object],
    fallback_day_index: int,
    request: PlanGenerateRequest,
    daily_limit: int,
) -> dict[str, object]:
    day_index = _first_int(
        value,
        fallback_day_index,
        "dayIndex",
        "day_index",
        "day",
        "index",
    )
    tasks = [
        _normalize_task(task, daily_limit, request)
        for task in _raw_tasks(value)
        if isinstance(task, dict)
    ]

    if not tasks:
        tasks = [
            task.model_dump()
            for task in _build_mock_day(day_index, request, daily_limit).tasks
        ]

    return {
        "dayIndex": day_index,
        "theme": _first_string(value, "theme", "focus", "title")
        or (
            f"第 {day_index} 天学习安排"
            if is_zh(request.responseLanguage)
            else f"Day {day_index} learning plan"
        ),
        "tasks": _fit_tasks_to_limit(tasks, daily_limit),
    }


def _normalize_task(
    value: dict[object, object],
    daily_limit: int,
    request: PlanGenerateRequest,
) -> dict[str, object]:
    minutes = _first_int(
        value,
        min(60, daily_limit),
        "estimatedMinutes",
        "estimated_minutes",
        "minutes",
        "durationMinutes",
        "duration",
    )
    return {
        "title": _first_string(value, "title", "name", "task")
        or ("聚焦任务" if is_zh(request.responseLanguage) else "Focused task"),
        "description": _first_string(value, "description", "details", "content")
        or (
            "完成计划中的学习、练习或应用任务。"
            if is_zh(request.responseLanguage)
            else "Complete the planned learning, practice, or application work."
        ),
        "estimatedMinutes": min(minutes, daily_limit),
        "type": _first_string(value, "type", "category", "kind") or "practice",
        "deliverable": _first_string(value, "deliverable", "output", "artifact")
        or ("阶段性学习产物" if is_zh(request.responseLanguage) else "Progress artifact"),
        "priority": _normalize_priority(
            _first_string(value, "priority", "urgency", "importance") or "medium"
        ),
    }


def _fit_tasks_to_limit(
    tasks: list[dict[str, object]],
    daily_limit: int,
) -> list[dict[str, object]]:
    total = sum(int(task["estimatedMinutes"]) for task in tasks)
    if total <= daily_limit:
        return tasks

    ratio = daily_limit / total
    fitted = []
    for task in tasks:
        adjusted = dict(task)
        adjusted["estimatedMinutes"] = max(15, int(int(task["estimatedMinutes"]) * ratio))
        fitted.append(adjusted)

    overflow = sum(int(task["estimatedMinutes"]) for task in fitted) - daily_limit
    while overflow > 0 and fitted:
        for task in reversed(fitted):
            if overflow <= 0:
                break
            if int(task["estimatedMinutes"]) > 15:
                task["estimatedMinutes"] = int(task["estimatedMinutes"]) - 1
                overflow -= 1

    return fitted


def _find_plan_object(parsed: dict[str, object]) -> dict[object, object]:
    for key in ("plan", "learningPlan", "learning_plan", "result", "data"):
        value = parsed.get(key)
        if isinstance(value, dict):
            return value
    return parsed


def _raw_days(values: dict[object, object]) -> list[object]:
    for key in ("days", "dailyPlans", "daily_plans", "schedule"):
        value = values.get(key)
        if isinstance(value, list):
            return value
    return []


def _raw_tasks(values: dict[object, object]) -> list[object]:
    for key in ("tasks", "items", "dailyTasks", "daily_tasks"):
        value = values.get(key)
        if isinstance(value, list):
            return value
    return []


def _string_list_from_value(value: object) -> list[str]:
    if isinstance(value, list):
        return [str(item).strip() for item in value if str(item).strip()]
    if isinstance(value, str) and value.strip():
        return [
            item.strip()
            for item in re.split(r"[,，;；\n]", value)
            if item.strip()
        ]
    return []


def _int_list(value: object) -> list[int]:
    if not isinstance(value, list):
        return []
    parsed: list[int] = []
    for item in value:
        if isinstance(item, int):
            parsed.append(max(1, item))
        elif isinstance(item, float):
            parsed.append(max(1, int(item)))
        elif isinstance(item, str):
            match = re.search(r"\d+", item)
            if match:
                parsed.append(max(1, int(match.group(0))))
    return parsed


def _first_string(values: dict[object, object], *keys: str) -> str:
    for key in keys:
        value = values.get(key)
        if isinstance(value, str) and value.strip():
            return value.strip()
        if isinstance(value, int | float):
            return str(value)
    return ""


def _first_int(values: dict[object, object], default: int, *keys: str) -> int:
    for key in keys:
        value = values.get(key)
        if isinstance(value, int):
            return max(1, value)
        if isinstance(value, float):
            return max(1, int(value))
        if isinstance(value, str):
            match = re.search(r"\d+", value)
            if match:
                return max(1, int(match.group(0)))
    return default


def _normalize_priority(value: str) -> str:
    normalized = value.strip().lower()
    if normalized in {"high", "urgent", "critical", "p0", "p1"}:
        return "high"
    if normalized in {"low", "optional", "p3"}:
        return "low"
    return "medium"


def _daily_minutes_limit(request: PlanGenerateRequest) -> int:
    if request.dailyAvailableHours and request.dailyAvailableHours > 0:
        return max(30, int(request.dailyAvailableHours * 60))
    return 120

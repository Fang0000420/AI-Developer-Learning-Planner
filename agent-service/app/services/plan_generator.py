import json
import math
import re

import httpx

from app.config import (
    DEEPSEEK_API_BASE_URL,
    DEEPSEEK_API_KEY,
    PROFILE_ANALYZER_MODEL,
    PROFILE_ANALYZER_TIMEOUT_SECONDS,
)
from app.schemas.plan import PlanDay, PlanGenerateRequest, PlanGenerateResponse, PlanTask
from app.services.language import is_zh, prompt_for
from app.services.model_retry import (
    ModelCallNonRetryableError,
    ModelCallRetryExhaustedError,
    retry_model_call,
)

PLAN_GENERATOR_PROMPT_EN = """
You are the Plan Generator for AI Developer Learning Planner.

Create a complete 14-day or 21-day learning and building plan. Return JSON only,
with this exact shape:
{
  "planTitle": "string",
  "durationDays": 14,
  "days": [
    {
      "dayIndex": 1,
      "theme": "string",
      "tasks": [
        {
          "title": "string",
          "description": "string",
          "estimatedMinutes": 60,
          "type": "learning|build|review|setup|test|deploy|documentation",
          "deliverable": "string",
          "priority": "high|medium|low"
        }
      ]
    }
  ]
}

Rules:
- Do not include markdown fences or explanatory prose.
- durationDays must match the requested durationDays.
- days must contain exactly one entry for every day from 1 to durationDays.
- Each day should have 2 or 3 focused tasks.
- The total estimatedMinutes for each day must not exceed dailyAvailableHours * 60.
- Every task must have title, description, estimatedMinutes, type, deliverable,
  and priority.
- The plan must build toward the recommended project and final deliverables.
- Keep tasks concrete, shippable, and suitable for an MVP.
""".strip()

PLAN_GENERATOR_PROMPT_ZH = """
你是 AI Developer Learning Planner 的计划生成器。

创建完整的 14 天或 21 天学习与项目构建计划。只返回 JSON，结构必须完全如下：
{
  "planTitle": "string",
  "durationDays": 14,
  "days": [
    {
      "dayIndex": 1,
      "theme": "string",
      "tasks": [
        {
          "title": "string",
          "description": "string",
          "estimatedMinutes": 60,
          "type": "learning|build|review|setup|test|deploy|documentation",
          "deliverable": "string",
          "priority": "high|medium|low"
        }
      ]
    }
  ]
}

规则：
- 不要包含 markdown 代码块或解释性正文。
- planTitle、theme、title、description、deliverable 必须使用简体中文。
- durationDays 必须匹配请求中的 durationDays。
- days 必须从 1 到 durationDays 每天一项。
- 每天应有 2 或 3 个聚焦任务。
- 每天 total estimatedMinutes 不得超过 dailyAvailableHours * 60。
- 每个任务必须包含 title、description、estimatedMinutes、type、deliverable 和 priority。
- 计划必须逐步走向推荐项目和最终交付物。
- 任务必须具体、可交付，并适合 MVP。
""".strip()


class PlanGeneratorError(RuntimeError):
    pass


def generate_plan(request: PlanGenerateRequest) -> PlanGenerateResponse:
    if DEEPSEEK_API_KEY:
        try:
            return retry_model_call(lambda: generate_plan_with_model(request))
        except ModelCallRetryExhaustedError:
            return generate_plan_with_mock(request)
        except ModelCallNonRetryableError as exc:
            raise PlanGeneratorError(f"Plan generator model call failed: {exc}") from exc

    return generate_plan_with_mock(request)


def generate_plan_with_model(request: PlanGenerateRequest) -> PlanGenerateResponse:
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
                        PLAN_GENERATOR_PROMPT_ZH,
                        PLAN_GENERATOR_PROMPT_EN,
                    ),
                },
                {
                    "role": "user",
                    "content": json.dumps(
                        {
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
    return PlanGenerateResponse.model_validate(normalized)


def generate_plan_with_mock(request: PlanGenerateRequest) -> PlanGenerateResponse:
    daily_limit = _daily_minutes_limit(request)
    days = [
        _build_mock_day(day_index, request, daily_limit)
        for day_index in range(1, request.durationDays + 1)
    ]
    return PlanGenerateResponse(
        planTitle=(
            f"{request.durationDays} 天 {request.recommendedProject} 构建计划"
            if is_zh(request.responseLanguage)
            else f"{request.durationDays}-Day {request.recommendedProject} Build Plan"
        ),
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
            ("基础", "明确需求、架构和本地环境。"),
            ("后端", "实现 Spring Boot API、持久化和校验。"),
            ("Agent", "构建 FastAPI Agent 接口和结构化输出处理。"),
            ("前端", "创建 Next.js 工作流和可用的规划视图。"),
            ("集成", "连接服务并验证完整学习闭环。"),
            ("收尾", "补充文档、测试，并准备可运行演示。"),
        ]
    else:
        phases = [
            ("Foundation", "Clarify requirements, architecture, and local setup."),
            ("Backend", "Implement Spring Boot APIs, persistence, and validation."),
            ("Agent", "Build FastAPI agent endpoints and structured output handling."),
            ("Frontend", "Create the Next.js workflow and useful planner views."),
            ("Integration", "Connect services and verify the full learning loop."),
            ("Polish", "Document, test, and prepare a runnable demo."),
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
                title=f"梳理第 {day_index} 天的 {focus} 范围",
                description="先复盘目标上下文、相关技能差距和预期交付物，再开始改代码。",
                estimatedMinutes=task_minutes[0],
                type="learning",
                deliverable=f"第 {day_index} 天范围说明",
                priority="high",
            ),
            PlanTask(
                title=f"构建 {focus}",
                description=f"实现 {request.recommendedProject} 中能推进今日主题的最小可用切片。",
                estimatedMinutes=task_minutes[1],
                type="build",
                deliverable=f"可运行的 {focus} 切片",
                priority="high",
            ),
        ]
    else:
        tasks = [
            PlanTask(
                title=f"Map Day {day_index} scope for {focus}",
                description=(
                    "Review the current goal context, relevant skill gaps, and expected "
                    "deliverables before changing code."
                ),
                estimatedMinutes=task_minutes[0],
                type="learning",
                deliverable=f"Day {day_index} scope notes",
                priority="high",
            ),
            PlanTask(
                title=f"Build {focus}",
                description=(
                    f"Implement the smallest useful slice of {request.recommendedProject} "
                    "that advances today's theme."
                ),
                estimatedMinutes=task_minutes[1],
                type="build",
                deliverable=f"Working {focus} slice",
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
                    "运行聚焦检查，记录缺口，并写下下一天需要继续的内容。"
                    if is_zh(request.responseLanguage)
                    else (
                        "Run focused checks, capture gaps, and write down what should "
                        "continue on the next day."
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
    return "planner MVP capability"


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
        or (
            f"{request.durationDays} 天 {request.recommendedProject} 构建计划"
            if is_zh(request.responseLanguage)
            else f"{request.durationDays}-Day {request.recommendedProject} Build Plan"
        ),
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
            f"第 {day_index} 天实现"
            if is_zh(request.responseLanguage)
            else f"Day {day_index} implementation"
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
            "完成计划中的学习和实现工作。"
            if is_zh(request.responseLanguage)
            else "Complete the planned learning and implementation work."
        ),
        "estimatedMinutes": min(minutes, daily_limit),
        "type": _first_string(value, "type", "category", "kind") or "build",
        "deliverable": _first_string(value, "deliverable", "output", "artifact")
        or ("可工作的阶段产物" if is_zh(request.responseLanguage) else "Working progress artifact"),
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

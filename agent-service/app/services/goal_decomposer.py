import json
import re

import httpx

from app.config import (
    DEEPSEEK_API_BASE_URL,
    DEEPSEEK_API_KEY,
    GOAL_DECOMPOSER_TIMEOUT_SECONDS,
    PROFILE_ANALYZER_MODEL,
)
from app.schemas.goal import GoalDecomposeRequest, GoalDecomposeResponse, SubGoal
from app.services.agent_execution import AgentExecutionResult, AgentResponseSource
from app.services.language import is_zh, prompt_for
from app.services.model_retry import (
    ModelCallNonRetryableError,
    ModelCallRetryExhaustedError,
    retry_model_call,
)

GOAL_DECOMPOSER_PROMPT_EN = """
You are the Goal Decomposer for AI Developer Learning Planner.

Decompose the learner's main goal into practical sub-goals for a structured
learning plan. Return JSON only, with this exact shape:
{
  "subGoals": [
    {
      "title": "string",
      "description": "string",
      "priority": "high | medium | low"
    }
  ]
}

Rules:
- Do not include markdown fences or explanatory prose.
- Generate 5 to 8 sub-goals.
- Each title must be concrete and action-oriented.
- Each description must explain what the learner should be able to do.
- priority must be exactly one of: high, medium, low.
- Use the optional background only to make the decomposition more realistic.
- Prefer domain-neutral learning language unless the goal is clearly technical.
""".strip()

GOAL_DECOMPOSER_PROMPT_ZH = """
你是 AI Developer Learning Planner 的目标拆解器。

把学习者的主目标拆解成适合结构化学习计划的实践子目标。只返回 JSON，
结构必须完全如下：
{
  "subGoals": [
    {
      "title": "string",
      "description": "string",
      "priority": "high | medium | low"
    }
  ]
}

规则：
- 不要包含 markdown 代码块或解释性正文。
- 所有 title 和 description 必须使用简体中文。
- 生成 5 到 8 个子目标。
- 每个 title 必须具体、行动导向。
- 每个 description 必须说明学习者最终应能做到什么。
- priority 必须是 high、medium、low 之一。
- 仅用可选背景让拆解更贴近真实情况。
- 除非目标明确属于技术领域，否则优先使用领域中立的学习表达。
""".strip()


class GoalDecomposerError(RuntimeError):
    pass


def decompose_goal(request: GoalDecomposeRequest) -> AgentExecutionResult[GoalDecomposeResponse]:
    if DEEPSEEK_API_KEY:
        try:
            return AgentExecutionResult(
                payload=retry_model_call(lambda: decompose_goal_with_model(request)),
                source=AgentResponseSource.MODEL,
            )
        except ModelCallRetryExhaustedError:
            return AgentExecutionResult(
                payload=decompose_goal_with_mock(request),
                source=AgentResponseSource.FALLBACK,
            )
        except ModelCallNonRetryableError as exc:
            raise GoalDecomposerError(f"Goal decomposer model call failed: {exc}") from exc

    return AgentExecutionResult(
        payload=decompose_goal_with_mock(request),
        source=AgentResponseSource.FALLBACK,
    )


def decompose_goal_with_model(request: GoalDecomposeRequest) -> GoalDecomposeResponse:
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
                        GOAL_DECOMPOSER_PROMPT_ZH,
                        GOAL_DECOMPOSER_PROMPT_EN,
                    ),
                },
                {
                    "role": "user",
                    "content": json.dumps(
                        {
                            "mainGoal": request.mainGoal,
                            "background": request.background,
                            "responseLanguage": request.responseLanguage,
                        },
                        ensure_ascii=False,
                    ),
                },
            ],
            "temperature": 0.2,
        },
        timeout=GOAL_DECOMPOSER_TIMEOUT_SECONDS,
    )
    response.raise_for_status()

    body = response.json()
    content = body["choices"][0]["message"]["content"]
    parsed = _load_json_object(content)
    normalized = _normalize_model_output(parsed, request)
    return GoalDecomposeResponse.model_validate(normalized)


def decompose_goal_with_mock(request: GoalDecomposeRequest) -> GoalDecomposeResponse:
    if is_zh(request.responseLanguage):
        return GoalDecomposeResponse(
            subGoals=[
                SubGoal(
                    title="明确目标能力地图",
                    description="把主目标拆成关键能力、可衡量标准和阶段性检查点。",
                    priority="high",
                ),
                SubGoal(
                    title="建立关键基础",
                    description="补齐支撑目标所需的基础知识、方法或常用框架。",
                    priority="high",
                ),
                SubGoal(
                    title="设计稳定练习机制",
                    description="安排可重复执行的练习方式，让学习能持续推进。",
                    priority="high",
                ),
                SubGoal(
                    title="产出阶段性成果",
                    description="用作品、演示、记录或测验证明学习成果正在形成。",
                    priority="medium",
                ),
                SubGoal(
                    title="复盘并调整下一阶段重点",
                    description=(
                        f"围绕目标「{request.mainGoal}」根据反馈持续调整节奏、难度和重点。"
                    ),
                    priority="medium",
                ),
            ]
        )

    return GoalDecomposeResponse(
        subGoals=[
            SubGoal(
                title="Clarify the target capability map",
                description=(
                    "Turn the main goal into required capabilities, measurable progress "
                    "signals, and stage checkpoints."
                ),
                priority="high",
            ),
            SubGoal(
                title="Build the essential foundation",
                description=(
                    "Strengthen the knowledge, methods, or baseline habits required to "
                    "support the goal."
                ),
                priority="high",
            ),
            SubGoal(
                title="Create a steady practice routine",
                description=(
                    "Set up repeatable practice sessions so progress can accumulate "
                    "consistently."
                ),
                priority="high",
            ),
            SubGoal(
                title="Produce visible evidence of progress",
                description=(
                    "Create outputs, recordings, notes, demos, or assessments that show "
                    "the skill is improving."
                ),
                priority="medium",
            ),
            SubGoal(
                title="Review and refine the next focus area",
                description=(
                    "Use feedback and results to refine the next stage for the goal: "
                    f"{request.mainGoal}."
                ),
                priority="medium",
            ),
        ]
    )


def _load_json_object(content: str) -> dict[str, object]:
    try:
        parsed = json.loads(content)
    except json.JSONDecodeError:
        match = re.search(r"\{.*\}", content, re.DOTALL)
        if match:
            parsed = json.loads(match.group(0))
        else:
            array_match = re.search(r"\[.*\]", content, re.DOTALL)
            if not array_match:
                raise
            parsed = json.loads(array_match.group(0))

    if isinstance(parsed, list):
        return {"subGoals": parsed}

    if not isinstance(parsed, dict):
        raise ValueError("Goal decomposer response must be a JSON object.")

    return parsed


def _normalize_model_output(
    parsed: dict[str, object],
    request: GoalDecomposeRequest,
) -> dict[str, list[dict[str, str]]]:
    raw_sub_goals = _find_sub_goal_items(parsed)
    normalized: list[dict[str, str]] = []

    if isinstance(raw_sub_goals, dict):
        raw_sub_goals = [
            {"title": str(title), "description": str(value)}
            if not isinstance(value, dict)
            else {"title": str(title), **value}
            for title, value in raw_sub_goals.items()
        ]

    if isinstance(raw_sub_goals, list):
        for item in raw_sub_goals:
            normalized_item = _normalize_sub_goal_item(item, request)
            if normalized_item and not _has_title(normalized, normalized_item["title"]):
                normalized.append(normalized_item)

    for fallback in _fallback_sub_goals(request):
        if len(normalized) >= 5:
            break
        if not _has_title(normalized, fallback["title"]):
            normalized.append(fallback)

    return {"subGoals": normalized}


def _find_sub_goal_items(parsed: dict[str, object]) -> object:
    direct_keys = (
        "subGoals",
        "sub_goals",
        "subgoals",
        "goals",
        "items",
        "milestones",
        "steps",
        "objectives",
        "decomposition",
        "result",
        "results",
        "data",
    )
    for key in direct_keys:
        value = parsed.get(key)
        if isinstance(value, list | dict):
            return value

    for value in parsed.values():
        if isinstance(value, dict):
            nested = _find_sub_goal_items(value)
            if isinstance(nested, list | dict):
                return nested

    return []


def _normalize_sub_goal_item(
    item: object,
    request: GoalDecomposeRequest,
) -> dict[str, str] | None:
    if isinstance(item, str):
        title = item.strip()
        if not title:
            return None
        return {
            "title": title,
            "description": f"Make measurable progress toward the goal: {request.mainGoal}.",
            "priority": "medium",
        }

    if not isinstance(item, dict):
        return None

    title = _first_string(
        item,
        "title",
        "name",
        "goal",
        "subGoal",
        "sub_goal",
        "objective",
        "step",
        "milestone",
        "标题",
        "子目标",
        "目标",
    )
    string_values = _string_values(item)
    if not title and string_values:
        title = string_values[0]
    if not title:
        return None

    description = _first_string(
        item,
        "description",
        "desc",
        "details",
        "detail",
        "content",
        "explanation",
        "outcome",
        "deliverable",
        "描述",
        "说明",
        "详情",
        "交付物",
    )
    if not description:
        description = _first_different_string(string_values, title) or (
            f"Build enough practical capability to support the goal: {request.mainGoal}."
        )

    priority = _normalize_priority(
        _first_string(
            item,
            "priority",
            "importance",
            "urgency",
            "level",
            "优先级",
            "重要性",
        )
    )

    return {
        "title": title,
        "description": description,
        "priority": priority,
    }


def _fallback_sub_goals(request: GoalDecomposeRequest) -> list[dict[str, str]]:
    mock_response = decompose_goal_with_mock(request)
    return [item.model_dump() for item in mock_response.subGoals]


def _first_string(values: dict[object, object], *keys: str) -> str:
    for key in keys:
        value = values.get(key)
        if isinstance(value, str) and value.strip():
            return value.strip()
        if isinstance(value, int | float):
            return str(value)
    return ""


def _string_values(values: dict[object, object]) -> list[str]:
    result: list[str] = []
    for value in values.values():
        if isinstance(value, str) and value.strip():
            result.append(value.strip())
        elif isinstance(value, int | float):
            result.append(str(value))
    return result


def _first_different_string(values: list[str], title: str) -> str:
    for value in values:
        if value.strip().lower() != title.strip().lower():
            return value
    return ""


def _normalize_priority(priority: str) -> str:
    normalized = priority.strip().lower()
    aliases = {
        "h": "high",
        "high": "high",
        "critical": "high",
        "urgent": "high",
        "important": "high",
        "高": "high",
        "高优先级": "high",
        "m": "medium",
        "medium": "medium",
        "moderate": "medium",
        "normal": "medium",
        "中": "medium",
        "中优先级": "medium",
        "l": "low",
        "low": "low",
        "minor": "low",
        "optional": "low",
        "低": "low",
        "低优先级": "low",
    }
    return aliases.get(normalized, "medium")


def _has_title(items: list[dict[str, str]], title: str) -> bool:
    return any(item["title"].strip().lower() == title.strip().lower() for item in items)

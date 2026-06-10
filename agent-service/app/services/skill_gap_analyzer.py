import json
import re

import httpx

from app.config import (
    DEEPSEEK_API_BASE_URL,
    DEEPSEEK_API_KEY,
    PROFILE_ANALYZER_MODEL,
    PROFILE_ANALYZER_TIMEOUT_SECONDS,
)
from app.schemas.skill_gap import (
    SkillGap,
    SkillGapAnalyzeRequest,
    SkillGapAnalyzeResponse,
)
from app.services.language import is_zh, prompt_for
from app.services.model_retry import (
    ModelCallNonRetryableError,
    ModelCallRetryExhaustedError,
    retry_model_call,
)

SKILL_GAP_ANALYZER_PROMPT_EN = """
You are the Skill Gap Analyzer for AI Developer Learning Planner.

Compare the learner's current profile with the decomposed goal. Return JSON
only, with this exact shape:
{
  "skillGaps": [
    {
      "skill": "string",
      "currentLevel": "string",
      "targetLevel": "string",
      "priority": "high | medium | low",
      "reason": "string"
    }
  ]
}

Rules:
- Do not include markdown fences or explanatory prose.
- Generate at least 4 skill gaps.
- Focus on practical developer capabilities that can drive project planning.
- currentLevel and targetLevel should be short labels such as beginner,
  basic, intermediate, advanced, or production-ready.
- priority must be exactly one of: high, medium, low.
- Use weaknesses and high-priority sub-goals to decide urgency.
""".strip()

SKILL_GAP_ANALYZER_PROMPT_ZH = """
你是 AI Developer Learning Planner 的技能差距分析器。

比较学习者当前画像和已拆解目标。只返回 JSON，结构必须完全如下：
{
  "skillGaps": [
    {
      "skill": "string",
      "currentLevel": "string",
      "targetLevel": "string",
      "priority": "high | medium | low",
      "reason": "string"
    }
  ]
}

规则：
- 不要包含 markdown 代码块或解释性正文。
- 所有自然语言字段值必须使用简体中文。
- 至少生成 4 个技能差距。
- 聚焦能驱动项目计划的开发者实践能力。
- currentLevel 和 targetLevel 使用简短标签。
- priority 必须是 high、medium、low 之一。
- 根据 weaknesses 和高优先级 subGoals 判断紧急程度。
""".strip()


class SkillGapAnalyzerError(RuntimeError):
    pass


def analyze_skill_gap(request: SkillGapAnalyzeRequest) -> SkillGapAnalyzeResponse:
    if DEEPSEEK_API_KEY:
        try:
            return retry_model_call(lambda: analyze_skill_gap_with_model(request))
        except ModelCallRetryExhaustedError:
            return analyze_skill_gap_with_mock(request)
        except ModelCallNonRetryableError as exc:
            raise SkillGapAnalyzerError(f"Skill gap analyzer model call failed: {exc}") from exc

    return analyze_skill_gap_with_mock(request)


def analyze_skill_gap_with_model(
    request: SkillGapAnalyzeRequest,
) -> SkillGapAnalyzeResponse:
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
                        SKILL_GAP_ANALYZER_PROMPT_ZH,
                        SKILL_GAP_ANALYZER_PROMPT_EN,
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
    return SkillGapAnalyzeResponse.model_validate(normalized)


def analyze_skill_gap_with_mock(
    request: SkillGapAnalyzeRequest,
) -> SkillGapAnalyzeResponse:
    if is_zh(request.responseLanguage):
        return SkillGapAnalyzeResponse(
            skillGaps=[
                SkillGap(
                    skill="AI Agent 工作流设计",
                    currentLevel="基础",
                    targetLevel="可用于生产",
                    priority="high",
                    reason="目标需要把复杂工作拆成可靠的 Agent 步骤，但当前画像尚未体现这类深度。",
                ),
                SkillGap(
                    skill="结构化 LLM 输出校验",
                    currentLevel="入门",
                    targetLevel="中级",
                    priority="high",
                    reason="画像、拆解和计划结果都需要严格 schema、校验和恢复路径才能驱动 MVP。",
                ),
                SkillGap(
                    skill="全栈规划链路集成",
                    currentLevel="基础",
                    targetLevel="中级",
                    priority="medium",
                    reason=(
                        "学习者需要把 Agent 输出接入后端持久化和 UI 流程，"
                        f"支撑目标「{request.mainGoal}」。"
                    ),
                ),
                SkillGap(
                    skill="评估与迭代闭环",
                    currentLevel="入门",
                    targetLevel="中级",
                    priority="medium",
                    reason="每日学习计划需要结合进度、质量信号和用户反馈持续调整。",
                ),
            ]
        )

    return SkillGapAnalyzeResponse(
        skillGaps=[
            SkillGap(
                skill="AI agent workflow design",
                currentLevel="basic",
                targetLevel="production-ready",
                priority="high",
                reason=(
                    "The goal requires decomposing work into reliable agent steps, "
                    "but the current profile does not yet show agent workflow depth."
                ),
            ),
            SkillGap(
                skill="Structured LLM output validation",
                currentLevel="beginner",
                targetLevel="intermediate",
                priority="high",
                reason=(
                    "Profile analysis and planning outputs need strict schemas, "
                    "validation, and recovery paths before they can drive the MVP."
                ),
            ),
            SkillGap(
                skill="Full-stack planning integration",
                currentLevel="basic",
                targetLevel="intermediate",
                priority="medium",
                reason=(
                    "The learner needs to connect agent outputs with backend "
                    f"persistence and UI flows for the goal: {request.mainGoal}."
                ),
            ),
            SkillGap(
                skill="Evaluation and iteration loop",
                currentLevel="beginner",
                targetLevel="intermediate",
                priority="medium",
                reason=(
                    "Daily learning plans should be checked against progress, "
                    "quality signals, and user feedback."
                ),
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
        return {"skillGaps": parsed}

    if not isinstance(parsed, dict):
        raise ValueError("Skill gap analyzer response must be a JSON object.")

    return parsed


def _normalize_model_output(
    parsed: dict[str, object],
    request: SkillGapAnalyzeRequest,
) -> dict[str, list[dict[str, str]]]:
    raw_skill_gaps = _find_skill_gap_items(parsed)
    normalized: list[dict[str, str]] = []

    if isinstance(raw_skill_gaps, dict):
        raw_skill_gaps = [
            {"skill": str(skill), **item}
            if isinstance(item, dict)
            else {"skill": str(skill), "reason": str(item)}
            for skill, item in raw_skill_gaps.items()
        ]

    if isinstance(raw_skill_gaps, list):
        for item in raw_skill_gaps:
            if not isinstance(item, dict):
                continue

            skill = _first_string(item, "skill", "name", "capability", "技能")
            if not skill:
                continue

            normalized.append(
                {
                    "skill": skill,
                    "currentLevel": _first_string(
                        item,
                        "currentLevel",
                        "current_level",
                        "current",
                        "currentSkillLevel",
                        "currentProficiency",
                        "current_proficiency",
                        "from",
                        "当前水平",
                    )
                    or "beginner",
                    "targetLevel": _first_string(
                        item,
                        "targetLevel",
                        "target_level",
                        "target",
                        "targetSkillLevel",
                        "targetProficiency",
                        "target_proficiency",
                        "to",
                        "目标水平",
                    )
                    or "intermediate",
                    "priority": _normalize_priority(
                        _first_string(item, "priority", "urgency", "优先级")
                    ),
                    "reason": _first_string(
                        item,
                        "reason",
                        "rationale",
                        "why",
                        "gap",
                        "description",
                        "原因",
                    )
                    or f"This skill is needed for the goal: {request.mainGoal}.",
                }
            )

    for fallback in _fallback_skill_gaps(request):
        if len(normalized) >= 4:
            break
        if not any(item["skill"].lower() == fallback["skill"].lower() for item in normalized):
            normalized.append(fallback)

    return {"skillGaps": normalized}


def _find_skill_gap_items(parsed: dict[str, object]) -> object:
    direct_keys = (
        "skillGaps",
        "skill_gaps",
        "skills",
        "gaps",
        "items",
        "results",
        "analysis",
    )
    for key in direct_keys:
        value = parsed.get(key)
        if isinstance(value, list | dict):
            return value

    for value in parsed.values():
        if isinstance(value, dict):
            nested = _find_skill_gap_items(value)
            if isinstance(nested, list | dict):
                return nested

    return []


def _fallback_skill_gaps(request: SkillGapAnalyzeRequest) -> list[dict[str, str]]:
    gaps: list[dict[str, str]] = []

    for weakness in request.weaknesses:
        if weakness:
            gaps.append(
                {
                    "skill": weakness,
                    "currentLevel": "beginner",
                    "targetLevel": "intermediate",
                    "priority": "high",
                    "reason": "This weakness directly limits progress toward the saved goal.",
                }
            )

    for sub_goal in request.subGoals:
        gaps.append(
            {
                "skill": sub_goal.title,
                "currentLevel": "basic",
                "targetLevel": "intermediate",
                "priority": _normalize_priority(sub_goal.priority),
                "reason": sub_goal.description,
            }
        )

    gaps.extend(
        [
            {
                "skill": "Goal-specific foundation",
                "currentLevel": "beginner",
                "targetLevel": "intermediate",
                "priority": "high",
                "reason": (
                    "The learner needs enough foundation to make progress on: "
                    f"{request.mainGoal}."
                ),
            },
            {
                "skill": "Structured practice routine",
                "currentLevel": "basic",
                "targetLevel": "intermediate",
                "priority": "medium",
                "reason": "A stable practice routine turns the goal into repeatable daily work.",
            },
            {
                "skill": "Feedback and evaluation",
                "currentLevel": "beginner",
                "targetLevel": "intermediate",
                "priority": "medium",
                "reason": "Progress needs regular checks so the plan can be adjusted.",
            },
            {
                "skill": "Applied output practice",
                "currentLevel": "basic",
                "targetLevel": "intermediate",
                "priority": "low",
                "reason": "The learner needs concrete outputs that prove the skill is improving.",
            },
        ]
    )

    return gaps


def _first_string(values: dict[object, object], *keys: str) -> str:
    for key in keys:
        value = values.get(key)
        if isinstance(value, str) and value.strip():
            return value.strip()
        if isinstance(value, int | float):
            return str(value)
    return ""


def _normalize_priority(priority: str) -> str:
    normalized = priority.strip().lower()
    aliases = {
        "h": "high",
        "high": "high",
        "critical": "high",
        "urgent": "high",
        "高": "high",
        "高优先级": "high",
        "m": "medium",
        "medium": "medium",
        "moderate": "medium",
        "中": "medium",
        "中优先级": "medium",
        "l": "low",
        "low": "low",
        "minor": "low",
        "低": "low",
        "低优先级": "low",
    }
    return aliases.get(normalized, "medium")

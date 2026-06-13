import json
import re

import httpx

from app.config import (
    DEEPSEEK_API_BASE_URL,
    DEEPSEEK_API_KEY,
    PROFILE_ANALYZER_MODEL,
    SKILL_GAP_ANALYZER_TIMEOUT_SECONDS,
)
from app.schemas.skill_gap import (
    SkillGap,
    SkillGapAnalyzeRequest,
    SkillGapAnalyzeResponse,
)
from app.services.agent_execution import AgentExecutionResult, AgentResponseSource
from app.services.cache.redis_json_cache import cache_get, cache_set
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
- Focus on the capability gaps that most directly affect progress toward the goal.
- currentLevel and targetLevel should be short labels such as beginner,
  basic, intermediate, advanced, or production-ready.
- priority must be exactly one of: high, medium, low.
- Use weaknesses and high-priority sub-goals to decide urgency.
- Prefer domain-neutral language unless the goal is clearly technical.
- If knowledgeContext is present, treat it as the learner's own materials
  and use it as personalized evidence.
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
- 聚焦最直接影响目标达成的能力差距。
- currentLevel 和 targetLevel 使用简短标签。
- priority 必须是 high、medium、low 之一。
- 根据 weaknesses 和高优先级 subGoals 判断紧急程度。
- 除非目标明确属于技术领域，否则优先使用领域中立的能力描述。
- 如果输入中提供 knowledgeContext，应把它视为学习者自己的资料和笔记，优先作为个性化证据。
""".strip()


class SkillGapAnalyzerError(RuntimeError):
    pass


def analyze_skill_gap(
    request: SkillGapAnalyzeRequest,
) -> AgentExecutionResult[SkillGapAnalyzeResponse]:
    if DEEPSEEK_API_KEY:
        try:
            return AgentExecutionResult(
                payload=retry_model_call(lambda: analyze_skill_gap_with_model(request)),
                source=AgentResponseSource.MODEL,
            )
        except ModelCallRetryExhaustedError:
            return AgentExecutionResult(
                payload=analyze_skill_gap_with_mock(request),
                source=AgentResponseSource.FALLBACK,
            )
        except ModelCallNonRetryableError as exc:
            raise SkillGapAnalyzerError(f"Skill gap analyzer model call failed: {exc}") from exc

    return AgentExecutionResult(
        payload=analyze_skill_gap_with_mock(request),
        source=AgentResponseSource.FALLBACK,
    )


def analyze_skill_gap_with_model(
    request: SkillGapAnalyzeRequest,
) -> SkillGapAnalyzeResponse:
    cache_payload = {
        "model": PROFILE_ANALYZER_MODEL,
        "request": request.model_dump(mode="json"),
    }
    cached = cache_get("skill-gap-analyzer", cache_payload)
    if cached is not None:
        return SkillGapAnalyzeResponse.model_validate(cached)

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
                            "knowledgeContext": request.knowledgeContext,
                            "responseLanguage": request.responseLanguage,
                        },
                        ensure_ascii=False,
                    ),
                },
            ],
            "temperature": 0.2,
        },
        timeout=SKILL_GAP_ANALYZER_TIMEOUT_SECONDS,
    )
    response.raise_for_status()

    body = response.json()
    content = body["choices"][0]["message"]["content"]
    parsed = _load_json_object(content)
    normalized = _normalize_model_output(parsed, request)
    cache_set("skill-gap-analyzer", cache_payload, normalized)
    return SkillGapAnalyzeResponse.model_validate(normalized)


def analyze_skill_gap_with_mock(
    request: SkillGapAnalyzeRequest,
) -> SkillGapAnalyzeResponse:
    if is_zh(request.responseLanguage):
        extra_reason = (
            " 这项判断也参考了用户知识库中的个人资料和笔记。"
            if request.knowledgeContext
            else ""
        )
        return SkillGapAnalyzeResponse(
            skillGaps=[
                SkillGap(
                    skill="目标相关基础",
                    currentLevel="基础",
                    targetLevel="熟练",
                    priority="high",
                    reason=(
                        "当前基础还不足以稳定支撑目标推进，需要先补齐核心认知和基本方法。"
                        + extra_reason
                    ),
                ),
                SkillGap(
                    skill="稳定练习机制",
                    currentLevel="入门",
                    targetLevel="中级",
                    priority="high",
                    reason="如果缺少固定节奏和明确练习方式，学习投入很难持续转化为进步。",
                ),
                SkillGap(
                    skill="应用与输出能力",
                    currentLevel="基础",
                    targetLevel="中级",
                    priority="medium",
                    reason=(
                        "学习者需要把所学内容转化为可展示、可验证的成果，"
                        f"以支撑目标「{request.mainGoal}」。"
                    ),
                ),
                SkillGap(
                    skill="反馈与迭代能力",
                    currentLevel="入门",
                    targetLevel="中级",
                    priority="medium",
                    reason="学习计划需要根据实际效果、阻塞点和反馈持续微调。",
                ),
            ]
        )

    extra_reason = (
        " This judgment also uses personal evidence from the learner's knowledge base."
        if request.knowledgeContext
        else ""
    )
    return SkillGapAnalyzeResponse(
        skillGaps=[
            SkillGap(
                skill="Goal-related foundation",
                currentLevel="basic",
                targetLevel="proficient",
                priority="high",
                reason=(
                    "The current foundation is not yet strong enough to support steady "
                    "progress toward the goal." + extra_reason
                ),
            ),
            SkillGap(
                skill="Consistent practice routine",
                currentLevel="beginner",
                targetLevel="intermediate",
                priority="high",
                reason=(
                    "Without a repeatable routine, learning effort is less likely to "
                    "turn into measurable progress."
                ),
            ),
            SkillGap(
                skill="Applied performance",
                currentLevel="basic",
                targetLevel="intermediate",
                priority="medium",
                reason=(
                    "The learner needs to turn practice into visible outcomes that "
                    f"support the goal: {request.mainGoal}."
                ),
            ),
            SkillGap(
                skill="Feedback and iteration loop",
                currentLevel="beginner",
                targetLevel="intermediate",
                priority="medium",
                reason=(
                    "Progress should be checked regularly so the plan can be adjusted "
                    "based on what is working."
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
                "skill": (
                    "目标相关基础"
                    if is_zh(request.responseLanguage)
                    else "Goal-related foundation"
                ),
                "currentLevel": "beginner",
                "targetLevel": "intermediate",
                "priority": "high",
                "reason": (
                    f"需要补齐支撑目标「{request.mainGoal}」的基础。"
                    if is_zh(request.responseLanguage)
                    else (
                        "The learner needs enough foundation to make progress on: "
                        f"{request.mainGoal}."
                    )
                ),
            },
            {
                "skill": (
                    "稳定练习机制"
                    if is_zh(request.responseLanguage)
                    else "Consistent practice routine"
                ),
                "currentLevel": "basic",
                "targetLevel": "intermediate",
                "priority": "medium",
                "reason": (
                    "稳定的练习机制能把目标转化为可持续的日常投入。"
                    if is_zh(request.responseLanguage)
                    else "A stable practice routine turns the goal into repeatable daily work."
                ),
            },
            {
                "skill": (
                    "反馈与评估"
                    if is_zh(request.responseLanguage)
                    else "Feedback and evaluation"
                ),
                "currentLevel": "beginner",
                "targetLevel": "intermediate",
                "priority": "medium",
                "reason": (
                    "需要定期检查学习效果，才能及时调整计划。"
                    if is_zh(request.responseLanguage)
                    else "Progress needs regular checks so the plan can be adjusted."
                ),
            },
            {
                "skill": (
                    "应用与输出"
                    if is_zh(request.responseLanguage)
                    else "Applied output practice"
                ),
                "currentLevel": "basic",
                "targetLevel": "intermediate",
                "priority": "low",
                "reason": (
                    "需要用具体成果证明能力正在提升。"
                    if is_zh(request.responseLanguage)
                    else "The learner needs concrete outputs that prove the skill is improving."
                ),
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

import json
import re

from app.config import DEEPSEEK_API_KEY, PROFILE_ANALYZER_MODEL, PROFILE_ANALYZER_TIMEOUT_SECONDS
from app.schemas.profile import ProfileAnalyzeRequest, ProfileAnalyzeResponse
from app.services.agent_execution import AgentExecutionResult, AgentResponseSource
from app.services.cache.redis_json_cache import cache_get, cache_set
from app.services.deepseek_chat import chat_completion_json
from app.services.language import is_zh
from app.services.model_retry import (
    ModelCallNonRetryableError,
    ModelCallRetryExhaustedError,
    retry_model_call,
)
from app.services.prompt_catalog import prompt_section


class ProfileAnalyzerError(RuntimeError):
    pass


def analyze_profile(request: ProfileAnalyzeRequest) -> AgentExecutionResult[ProfileAnalyzeResponse]:
    if DEEPSEEK_API_KEY:
        try:
            return AgentExecutionResult(
                payload=retry_model_call(lambda: analyze_profile_with_model(request)),
                source=AgentResponseSource.MODEL,
            )
        except ModelCallRetryExhaustedError:
            return AgentExecutionResult(
                payload=analyze_profile_with_mock(request),
                source=AgentResponseSource.FALLBACK,
            )
        except ModelCallNonRetryableError as exc:
            raise ProfileAnalyzerError(f"Profile analyzer model call failed: {exc}") from exc

    return AgentExecutionResult(
        payload=analyze_profile_with_mock(request),
        source=AgentResponseSource.FALLBACK,
    )


def analyze_profile_with_model(request: ProfileAnalyzeRequest) -> ProfileAnalyzeResponse:
    cache_payload = {
        "model": PROFILE_ANALYZER_MODEL,
        "request": request.model_dump(mode="json"),
    }
    cached = cache_get("profile-analyzer", cache_payload)
    if cached is not None:
        return ProfileAnalyzeResponse.model_validate(cached)

    parsed = chat_completion_json(
        model=PROFILE_ANALYZER_MODEL,
        messages=[
            {
                "role": "system",
                "content": prompt_section(
                    "profile_analyzer",
                    "system_rules",
                    request.responseLanguage,
                ),
            },
            {
                "role": "user",
                "content": json.dumps(
                    {
                        "background": request.background,
                        "goal": request.goal,
                        "dailyAvailableHours": request.dailyAvailableHours,
                        "knowledgeContext": request.knowledgeContext,
                        "responseLanguage": request.responseLanguage,
                    },
                    ensure_ascii=False,
                ),
            },
        ],
        timeout_seconds=PROFILE_ANALYZER_TIMEOUT_SECONDS,
        temperature=0.2,
        max_tokens=1000,
    )
    normalized = _normalize_model_output(parsed, request)
    cache_set("profile-analyzer", cache_payload, normalized)
    return ProfileAnalyzeResponse.model_validate(normalized)


def analyze_profile_with_mock(request: ProfileAnalyzeRequest) -> ProfileAnalyzeResponse:
    if is_zh(request.responseLanguage):
        direction_suffix = (
            " 并优先参考用户知识库中的个人资料与学习笔记。"
            if request.knowledgeContext
            else ""
        )
        return ProfileAnalyzeResponse(
            currentSkills=[
                "目标相关基础认知",
                "已有学习经验",
                "可迁移的实践能力",
            ],
            strengths=[
                "学习目标明确",
                "愿意持续投入时间",
                "具备一定自我驱动能力",
            ],
            weaknesses=[
                "系统化练习机制",
                "成果验证方式",
                "反馈与迭代节奏",
            ],
            recommendedDirection=(
                f"围绕目标「{request.goal}」先明确关键能力要求，再建立稳定练习、"
                "阶段性输出和复盘调整的学习闭环。"
                + direction_suffix
            ),
        )

    direction_suffix = (
        " Use the personal knowledge-base excerpts as higher-priority evidence."
        if request.knowledgeContext
        else ""
    )
    return ProfileAnalyzeResponse(
        currentSkills=[
            "Goal-related fundamentals",
            "Existing learning experience",
            "Transferable practical skills",
        ],
        strengths=[
            "Clear learning goal",
            "Willingness to invest time consistently",
            "Some self-directed learning ability",
        ],
        weaknesses=[
            "Systematic practice routine",
            "Evidence of progress",
            "Feedback and iteration rhythm",
        ],
        recommendedDirection=(
            "Clarify the capabilities required for the goal first, then build a steady "
            f"cycle of practice, tangible outputs, and review around: {request.goal}."
            + direction_suffix
        ),
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
        raise ValueError("Profile analyzer response must be a JSON object.")

    return parsed


def _normalize_model_output(
    parsed: dict[str, object],
    request: ProfileAnalyzeRequest,
) -> dict[str, object]:
    nested = _find_profile_object(parsed)
    fallback = analyze_profile_with_mock(request)
    return {
        "currentSkills": _string_list(
            nested,
            "currentSkills",
            "current_skills",
            "skills",
            "capabilities",
            "existingSkills",
            "existing_skills",
            "当前技能",
            "技能",
        )
        or fallback.currentSkills,
        "strengths": _string_list(
            nested,
            "strengths",
            "advantages",
            "pros",
            "优点",
            "优势",
        )
        or fallback.strengths,
        "weaknesses": _string_list(
            nested,
            "weaknesses",
            "gaps",
            "limitations",
            "cons",
            "不足",
            "短板",
        )
        or fallback.weaknesses,
        "recommendedDirection": _first_string(
            nested,
            "recommendedDirection",
            "recommended_direction",
            "direction",
            "recommendation",
            "建议方向",
            "方向",
        )
        or fallback.recommendedDirection,
    }


def _find_profile_object(parsed: dict[str, object]) -> dict[object, object]:
    for key in ("profile", "analysis", "result", "data"):
        value = parsed.get(key)
        if isinstance(value, dict):
            return value
    return parsed


def _string_list(values: dict[object, object], *keys: str) -> list[str]:
    for key in keys:
        value = values.get(key)
        if isinstance(value, list):
            cleaned = [str(item).strip() for item in value if str(item).strip()]
            if cleaned:
                return cleaned
        if isinstance(value, str) and value.strip():
            return [
                item.strip()
                for item in re.split(r"[,，;；\n]", value)
                if item.strip()
            ]
    return []


def _first_string(values: dict[object, object], *keys: str) -> str:
    for key in keys:
        value = values.get(key)
        if isinstance(value, str) and value.strip():
            return value.strip()
        if isinstance(value, int | float):
            return str(value)
    return ""

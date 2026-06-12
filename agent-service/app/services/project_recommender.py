import json
import re

from app.config import (
    DEEPSEEK_API_KEY,
    PROJECT_RECOMMENDER_MODEL,
    PROJECT_RECOMMENDER_TIMEOUT_SECONDS,
)
from app.schemas.project import (
    ProjectCandidateComparison,
    ProjectRecommendationMemory,
    ProjectRecommendRequest,
    ProjectRecommendResponse,
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


class ProjectRecommenderError(RuntimeError):
    pass


def recommend_project(
    request: ProjectRecommendRequest,
) -> AgentExecutionResult[ProjectRecommendResponse]:
    if DEEPSEEK_API_KEY:
        try:
            return AgentExecutionResult(
                payload=retry_model_call(lambda: recommend_project_with_model(request)),
                source=AgentResponseSource.MODEL,
            )
        except ModelCallRetryExhaustedError:
            return AgentExecutionResult(
                payload=recommend_project_with_mock(request),
                source=AgentResponseSource.FALLBACK,
            )
        except ModelCallNonRetryableError as exc:
            raise ProjectRecommenderError(f"Project recommender model call failed: {exc}") from exc

    return AgentExecutionResult(
        payload=recommend_project_with_mock(request),
        source=AgentResponseSource.FALLBACK,
    )


def recommend_project_with_model(
    request: ProjectRecommendRequest,
) -> ProjectRecommendResponse:
    summary = ProjectRecommendationMemory.model_validate(
        chat_completion_json(
            model=PROJECT_RECOMMENDER_MODEL,
            messages=_summary_round_messages(request),
            timeout_seconds=PROJECT_RECOMMENDER_TIMEOUT_SECONDS,
            temperature=0.2,
            max_tokens=900,
        )
    )
    comparison = ProjectCandidateComparison.model_validate(
        chat_completion_json(
            model=PROJECT_RECOMMENDER_MODEL,
            messages=_comparison_round_messages(request, summary),
            timeout_seconds=PROJECT_RECOMMENDER_TIMEOUT_SECONDS,
            temperature=0.2,
            max_tokens=1200,
        )
    )
    parsed = chat_completion_json(
        model=PROJECT_RECOMMENDER_MODEL,
        messages=_final_round_messages(request, summary, comparison),
        timeout_seconds=PROJECT_RECOMMENDER_TIMEOUT_SECONDS,
        temperature=0.2,
        max_tokens=1200,
    )
    normalized = _normalize_model_output(parsed, request)
    return ProjectRecommendResponse.model_validate(normalized)


def _summary_round_messages(
    request: ProjectRecommendRequest,
) -> list[dict[str, str]]:
    return [
        {
            "role": "system",
            "content": prompt_section(
                "project_recommender",
                "system_rules",
                request.responseLanguage,
            ),
        },
        {
            "role": "user",
            "content": (
                prompt_section(
                    "project_recommender",
                    "summary_instruction",
                    request.responseLanguage,
                )
                + "\n"
                + json.dumps(_project_context(request), ensure_ascii=False)
            ),
        },
    ]


def _comparison_round_messages(
    request: ProjectRecommendRequest,
    summary: ProjectRecommendationMemory,
) -> list[dict[str, str]]:
    return [
        {
            "role": "system",
            "content": prompt_section(
                "project_recommender",
                "system_rules",
                request.responseLanguage,
            ),
        },
        {
            "role": "assistant",
            "content": json.dumps(summary.model_dump(), ensure_ascii=False),
        },
        {
            "role": "user",
            "content": (
                prompt_section(
                    "project_recommender",
                    "comparison_instruction",
                    request.responseLanguage,
                )
                + "\n"
                + json.dumps(_project_context(request), ensure_ascii=False)
            ),
        },
    ]


def _final_round_messages(
    request: ProjectRecommendRequest,
    summary: ProjectRecommendationMemory,
    comparison: ProjectCandidateComparison,
) -> list[dict[str, str]]:
    return [
        {
            "role": "system",
            "content": prompt_section(
                "project_recommender",
                "system_rules",
                request.responseLanguage,
            ),
        },
        {
            "role": "assistant",
            "content": json.dumps(summary.model_dump(), ensure_ascii=False),
        },
        {
            "role": "assistant",
            "content": json.dumps(comparison.model_dump(), ensure_ascii=False),
        },
        {
            "role": "user",
            "content": (
                prompt_section(
                    "project_recommender",
                    "final_instruction",
                    request.responseLanguage,
                )
                + "\n"
                + json.dumps(_project_context(request), ensure_ascii=False)
            ),
        },
    ]


def _project_context(request: ProjectRecommendRequest) -> dict[str, object]:
    return {
        "mainGoal": request.mainGoal,
        "currentSkills": request.currentSkills,
        "strengths": request.strengths,
        "weaknesses": request.weaknesses,
        "subGoals": [item.model_dump() for item in request.subGoals],
        "skillGaps": [item.model_dump() for item in request.skillGaps],
        "durationDays": request.durationDays,
        "dailyAvailableHours": request.dailyAvailableHours,
        "responseLanguage": request.responseLanguage,
    }


def recommend_project_with_mock(
    request: ProjectRecommendRequest,
) -> ProjectRecommendResponse:
    daily_hours = _daily_hours(request)
    if is_zh(request.responseLanguage):
        return ProjectRecommendResponse(
            recommendedProject=_fallback_project_title(request),
            reason=(
                "这条主线能把学习目标转化为持续练习、阶段性输出和可验证成果，"
                "既方便聚焦重点，也便于根据反馈逐步调整。"
            ),
            difficulty="中等",
            durationDays=request.durationDays,
            dailyTimeHours=daily_hours,
            coreTechStack=[
                "关键基础",
                "稳定练习",
                "场景应用",
                "反馈复盘",
            ],
            finalDeliverables=[
                "阶段性成果记录",
                "可展示的练习输出",
                "复盘笔记",
                "下一阶段调整依据",
            ],
        )

    return ProjectRecommendResponse(
        recommendedProject=_fallback_project_title(request),
        reason=(
            "This track turns the learner's goal into a focused path with repeatable "
            "practice, visible outputs, and clear signals for adjustment."
        ),
        difficulty="medium",
        durationDays=request.durationDays,
        dailyTimeHours=daily_hours,
        coreTechStack=[
            "Core foundation",
            "Consistent practice",
            "Applied scenarios",
            "Feedback review",
        ],
        finalDeliverables=[
            "Stage progress notes",
            "Visible practice outputs",
            "Review summary",
            "Refined next-step focus",
        ],
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
        raise ValueError("Project recommender response must be a JSON object.")

    return parsed


def _normalize_model_output(
    parsed: dict[str, object],
    request: ProjectRecommendRequest,
) -> dict[str, object]:
    nested = _find_project_object(parsed)
    return {
        "recommendedProject": _first_string(
            nested,
            "recommendedProject",
            "recommended_track",
            "recommendedTrack",
            "projectName",
            "project",
            "title",
            "name",
            "学习主线",
            "主线",
            "方向",
        )
        or _fallback_project_title(request),
        "reason": _first_string(
            nested,
            "reason",
            "rationale",
            "why",
            "description",
            "推荐理由",
        )
        or (
            "This recommendation is a good fit because it turns the goal into a focused "
            "learning path with clear practice and visible outcomes."
        ),
        "difficulty": _first_string(
            nested,
            "difficulty",
            "level",
            "complexity",
            "难度",
        )
        or ("中等" if is_zh(request.responseLanguage) else "medium"),
        "durationDays": _first_int(
            nested,
            request.durationDays,
            "durationDays",
            "duration_days",
            "duration",
            "周期天数",
        ),
        "dailyTimeHours": _first_float(
            nested,
            _daily_hours(request),
            "dailyTimeHours",
            "daily_time_hours",
            "dailyAvailableHours",
            "daily_available_hours",
            "每日时间",
        ),
        "coreTechStack": _string_list(
            nested,
            "coreTechStack",
            "core_tech_stack",
            "techStack",
            "tech_stack",
            "technologies",
            "技术栈",
        )
        or _fallback_tech_stack(request),
        "finalDeliverables": _string_list(
            nested,
            "finalDeliverables",
            "final_deliverables",
            "deliverables",
            "outputs",
            "交付物",
        )
        or _fallback_deliverables(request),
    }


def _find_project_object(parsed: dict[str, object]) -> dict[object, object]:
    for key in ("recommendation", "projectRecommendation", "project", "result", "data"):
        value = parsed.get(key)
        if isinstance(value, dict):
            return value
    return parsed


def _fallback_tech_stack(request: ProjectRecommendRequest) -> list[str]:
    values = (
        ["关键基础", "稳定练习", "场景应用", "反馈复盘"]
        if is_zh(request.responseLanguage)
        else [
            "Core foundation",
            "Consistent practice",
            "Applied scenarios",
            "Feedback review",
        ]
    )
    for skill_gap in request.skillGaps:
        if skill_gap.skill and skill_gap.skill not in values:
            values.append(skill_gap.skill)
        if len(values) >= 6:
            break
    return values


def _fallback_deliverables(request: ProjectRecommendRequest) -> list[str]:
    if is_zh(request.responseLanguage):
        return [
            "阶段性成果记录",
            "可展示的练习输出",
            "复盘笔记",
            "下一阶段调整依据",
        ]
    return [
        "Stage progress notes",
        "Visible practice outputs",
        "Review summary",
        "Refined next-step focus",
    ]


def _fallback_project_title(request: ProjectRecommendRequest) -> str:
    if is_zh(request.responseLanguage):
        return f"{request.mainGoal} 学习主线"
    return f"{request.mainGoal} learning track"


def _string_list(values: dict[object, object], *keys: str) -> list[str]:
    for key in keys:
        value = values.get(key)
        if isinstance(value, list):
            return [str(item).strip() for item in value if str(item).strip()]
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


def _first_int(values: dict[object, object], default: int, *keys: str) -> int:
    for key in keys:
        value = values.get(key)
        if isinstance(value, int):
            return max(1, value)
        if isinstance(value, float):
            return max(1, int(value))
        if isinstance(value, str) and value.strip().isdigit():
            return max(1, int(value.strip()))
    return default


def _first_float(values: dict[object, object], default: float, *keys: str) -> float:
    for key in keys:
        value = values.get(key)
        if isinstance(value, int | float):
            return max(0.5, float(value))
        if isinstance(value, str):
            match = re.search(r"\d+(?:\.\d+)?", value)
            if match:
                return max(0.5, float(match.group(0)))
    return default


def _daily_hours(request: ProjectRecommendRequest) -> float:
    if request.dailyAvailableHours and request.dailyAvailableHours > 0:
        return request.dailyAvailableHours
    return 2.0

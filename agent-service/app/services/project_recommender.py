import json
import re

import httpx

from app.config import (
    DEEPSEEK_API_BASE_URL,
    DEEPSEEK_API_KEY,
    PROFILE_ANALYZER_MODEL,
    PROFILE_ANALYZER_TIMEOUT_SECONDS,
)
from app.schemas.project import ProjectRecommendRequest, ProjectRecommendResponse
from app.services.language import is_zh, prompt_for
from app.services.model_retry import (
    ModelCallNonRetryableError,
    ModelCallRetryExhaustedError,
    retry_model_call,
)

PROJECT_RECOMMENDER_PROMPT_EN = """
You are the Project Recommender for AI Developer Learning Planner.

Recommend exactly one MVP project direction for the learner. Return JSON only,
with this exact shape:
{
  "recommendedProject": "string",
  "reason": "string",
  "difficulty": "string",
  "durationDays": 21,
  "dailyTimeHours": 2,
  "coreTechStack": ["string"],
  "finalDeliverables": ["string"]
}

Rules:
- Do not include markdown fences or explanatory prose.
- Prefer the recommended project name: AI Developer Learning Planner.
- Use the profile, sub-goals, skill gaps, durationDays, and dailyAvailableHours
  to tailor the reason, difficulty, tech stack, and deliverables.
- Do not include a daily implementation plan or bonus/stretch work.
- durationDays should match the requested durationDays unless it is impossible.
- dailyTimeHours should match dailyAvailableHours when provided.
""".strip()

PROJECT_RECOMMENDER_PROMPT_ZH = """
你是 AI Developer Learning Planner 的项目推荐器。

为学习者推荐且只推荐一个 MVP 项目方向。只返回 JSON，结构必须完全如下：
{
  "recommendedProject": "string",
  "reason": "string",
  "difficulty": "string",
  "durationDays": 21,
  "dailyTimeHours": 2,
  "coreTechStack": ["string"],
  "finalDeliverables": ["string"]
}

规则：
- 不要包含 markdown 代码块或解释性正文。
- 推荐项目名优先使用：AI Developer Learning Planner。
- reason、difficulty、finalDeliverables 中的自然语言必须使用简体中文。
- 根据 profile、subGoals、skillGaps、durationDays 和 dailyAvailableHours
  调整推荐理由、难度、技术栈和交付物。
- 不要包含每日实施计划或额外拓展任务。
- durationDays 应匹配请求中的 durationDays。
- dailyTimeHours 应在提供 dailyAvailableHours 时与其一致。
""".strip()


class ProjectRecommenderError(RuntimeError):
    pass


def recommend_project(request: ProjectRecommendRequest) -> ProjectRecommendResponse:
    if DEEPSEEK_API_KEY:
        try:
            return retry_model_call(lambda: recommend_project_with_model(request))
        except ModelCallRetryExhaustedError:
            return recommend_project_with_mock(request)
        except ModelCallNonRetryableError as exc:
            raise ProjectRecommenderError(f"Project recommender model call failed: {exc}") from exc

    return recommend_project_with_mock(request)


def recommend_project_with_model(
    request: ProjectRecommendRequest,
) -> ProjectRecommendResponse:
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
                        PROJECT_RECOMMENDER_PROMPT_ZH,
                        PROJECT_RECOMMENDER_PROMPT_EN,
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
        timeout=PROJECT_RECOMMENDER_TIMEOUT_SECONDS,
    )
    response.raise_for_status()

    body = response.json()
    content = body["choices"][0]["message"]["content"]
    parsed = _load_json_object(content)
    normalized = _normalize_model_output(parsed, request)
    return ProjectRecommendResponse.model_validate(normalized)


def recommend_project_with_mock(
    request: ProjectRecommendRequest,
) -> ProjectRecommendResponse:
    daily_hours = _daily_hours(request)
    if is_zh(request.responseLanguage):
        return ProjectRecommendResponse(
            recommendedProject="AI Developer Learning Planner",
            reason=(
                "这个项目能把学习目标落到一个具体的全栈 AI Agent MVP 中，"
                "同时练习后端 API、FastAPI Agent 服务、结构化模型输出、"
                "数据库持久化和面向用户的规划界面。"
            ),
            difficulty="中高",
            durationDays=request.durationDays,
            dailyTimeHours=daily_hours,
            coreTechStack=[
                "Spring Boot",
                "FastAPI",
                "DeepSeek",
                "PostgreSQL",
                "Next.js",
                "Docker",
            ],
            finalDeliverables=[
                "完整 GitHub 仓库",
                "可运行的全栈演示",
                "PostgreSQL 中的 Agent 执行记录",
                "README 和架构文档",
                "可部署的服务配置",
            ],
        )

    return ProjectRecommendResponse(
        recommendedProject="AI Developer Learning Planner",
        reason=(
            "This project turns the learner's goal into a concrete full-stack AI "
            "agent MVP, exercising backend APIs, FastAPI agent services, "
            "structured model outputs, persistence, and a user-facing planner UI."
        ),
        difficulty="medium-high",
        durationDays=request.durationDays,
        dailyTimeHours=daily_hours,
        coreTechStack=[
            "Spring Boot",
            "FastAPI",
            "DeepSeek",
            "PostgreSQL",
            "Next.js",
            "Docker",
        ],
        finalDeliverables=[
            "Complete GitHub repository",
            "Runnable full-stack demo",
            "Agent execution records in PostgreSQL",
            "README and architecture documentation",
            "Deployable service configuration",
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
        "recommendedProject": "AI Developer Learning Planner",
        "reason": _first_string(
            nested,
            "reason",
            "rationale",
            "why",
            "description",
            "推荐理由",
        )
        or (
            "This project is a good fit because it converts the learning goal "
            "into a shippable AI developer workflow."
        ),
        "difficulty": _first_string(
            nested,
            "difficulty",
            "level",
            "complexity",
            "难度",
        )
        or "medium-high",
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
        or _fallback_deliverables(),
    }


def _find_project_object(parsed: dict[str, object]) -> dict[object, object]:
    for key in ("recommendation", "projectRecommendation", "project", "result", "data"):
        value = parsed.get(key)
        if isinstance(value, dict):
            return value
    return parsed


def _fallback_tech_stack(request: ProjectRecommendRequest) -> list[str]:
    values = [
        "Spring Boot",
        "FastAPI",
        "DeepSeek",
        "PostgreSQL",
        "Next.js",
        "Docker",
    ]
    for skill_gap in request.skillGaps:
        if skill_gap.skill and skill_gap.skill not in values:
            values.append(skill_gap.skill)
        if len(values) >= 8:
            break
    return values


def _fallback_deliverables() -> list[str]:
    return [
        "Complete GitHub repository",
        "Runnable full-stack demo",
        "Agent execution records in PostgreSQL",
        "README and architecture documentation",
        "Deployable service configuration",
    ]


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

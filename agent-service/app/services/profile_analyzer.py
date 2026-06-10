import json
import re

import httpx

from app.config import (
    DEEPSEEK_API_BASE_URL,
    DEEPSEEK_API_KEY,
    PROFILE_ANALYZER_MODEL,
    PROFILE_ANALYZER_TIMEOUT_SECONDS,
)
from app.schemas.profile import ProfileAnalyzeRequest, ProfileAnalyzeResponse
from app.services.language import is_zh, prompt_for
from app.services.model_retry import (
    ModelCallNonRetryableError,
    ModelCallRetryExhaustedError,
    retry_model_call,
)

PROFILE_ANALYZER_PROMPT_EN = """
You are the Profile Analyzer for AI Developer Learning Planner.

Analyze a learner's technical background and goal. Return JSON only, with these
exact field names:
- currentSkills: string[]
- strengths: string[]
- weaknesses: string[]
- recommendedDirection: string

Rules:
- Do not include markdown fences or explanatory prose.
- Keep each list focused and practical for an MVP learning plan.
- Infer skills only from the user's background and goal.
- recommendedDirection should be one concise paragraph.
""".strip()

PROFILE_ANALYZER_PROMPT_ZH = """
你是 AI Developer Learning Planner 的能力画像分析器。

分析学习者的技术背景和目标。只返回 JSON，字段名必须完全如下：
- currentSkills: string[]
- strengths: string[]
- weaknesses: string[]
- recommendedDirection: string

规则：
- 不要包含 markdown 代码块或解释性正文。
- 所有字段值中的自然语言必须使用简体中文。
- 每个列表都要聚焦 MVP 学习计划中真正实用的能力。
- 只能根据用户背景和目标推断技能。
- recommendedDirection 应是一段简洁的中文建议。
""".strip()


class ProfileAnalyzerError(RuntimeError):
    pass


def analyze_profile(request: ProfileAnalyzeRequest) -> ProfileAnalyzeResponse:
    if DEEPSEEK_API_KEY:
        try:
            return retry_model_call(lambda: analyze_profile_with_model(request))
        except ModelCallRetryExhaustedError:
            return analyze_profile_with_mock(request)
        except ModelCallNonRetryableError as exc:
            raise ProfileAnalyzerError(f"Profile analyzer model call failed: {exc}") from exc

    return analyze_profile_with_mock(request)


def analyze_profile_with_model(request: ProfileAnalyzeRequest) -> ProfileAnalyzeResponse:
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
                        PROFILE_ANALYZER_PROMPT_ZH,
                        PROFILE_ANALYZER_PROMPT_EN,
                    ),
                },
                {
                    "role": "user",
                    "content": json.dumps(
                        {
                            "background": request.background,
                            "goal": request.goal,
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
    return ProfileAnalyzeResponse.model_validate(_load_json_object(content))


def analyze_profile_with_mock(request: ProfileAnalyzeRequest) -> ProfileAnalyzeResponse:
    if is_zh(request.responseLanguage):
        return ProfileAnalyzeResponse(
            currentSkills=[
                "Python 基础",
                "后端开发基础",
                "REST API 设计",
            ],
            strengths=[
                "学习目标清晰",
                "每日时间投入稳定",
                "具备软件工程基础",
            ],
            weaknesses=[
                "AI Agent 工作流设计",
                "LLM 应用评估",
                "端到端项目集成",
            ],
            recommendedDirection=(
                "先构建一个 FastAPI Agent 服务，再把它接入后端流程，围绕目标"
                f"「{request.goal}」形成可演示的完整闭环。"
            ),
        )

    return ProfileAnalyzeResponse(
        currentSkills=[
            "Python basics",
            "Backend development fundamentals",
            "REST API design",
        ],
        strengths=[
            "Clear learning goal",
            "Consistent daily time budget",
            "Software engineering foundation",
        ],
        weaknesses=[
            "AI agent workflow design",
            "LLM application evaluation",
            "End-to-end project integration",
        ],
        recommendedDirection=(
            "Build a FastAPI-based AI planning service first, then connect it with the "
            f"backend workflow for the goal: {request.goal}."
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

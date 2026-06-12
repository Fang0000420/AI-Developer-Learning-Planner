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
from app.services.agent_execution import AgentExecutionResult, AgentResponseSource
from app.services.language import is_zh, prompt_for
from app.services.model_retry import (
    ModelCallNonRetryableError,
    ModelCallRetryExhaustedError,
    retry_model_call,
)

PROFILE_ANALYZER_PROMPT_EN = """
You are the Profile Analyzer for AI Developer Learning Planner.

Analyze a learner's background, existing capabilities, and learning goal. Return
JSON only, with these exact field names:
- currentSkills: string[]
- strengths: string[]
- weaknesses: string[]
- recommendedDirection: string

Rules:
- Do not include markdown fences or explanatory prose.
- Keep each list focused, concrete, and useful for a structured learning plan.
- Infer capabilities only from the user's background and goal.
- Prefer domain-neutral language unless the goal is clearly technical.
- recommendedDirection should be one concise paragraph describing a suitable
  learning direction.
""".strip()

PROFILE_ANALYZER_PROMPT_ZH = """
你是 AI Developer Learning Planner 的能力画像分析器。

分析学习者的能力背景、已有基础与学习目标。只返回 JSON，字段名必须完全如下：
- currentSkills: string[]
- strengths: string[]
- weaknesses: string[]
- recommendedDirection: string

规则：
- 不要包含 markdown 代码块或解释性正文。
- 所有字段值中的自然语言必须使用简体中文。
- 每个列表都要聚焦结构化学习计划中真正实用的能力。
- 只能根据用户背景和目标推断能力，不要自行补充领域设定。
- 除非目标明确属于技术领域，否则优先使用领域中立的能力描述。
- recommendedDirection 应是一段简洁的中文建议。
""".strip()


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
            ),
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

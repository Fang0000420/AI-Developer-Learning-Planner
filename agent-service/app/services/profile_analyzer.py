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
from app.services.model_retry import (
    ModelCallNonRetryableError,
    ModelCallRetryExhaustedError,
    retry_model_call,
)

PROFILE_ANALYZER_PROMPT = """
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
                {"role": "system", "content": PROFILE_ANALYZER_PROMPT},
                {
                    "role": "user",
                    "content": json.dumps(
                        {
                            "background": request.background,
                            "goal": request.goal,
                            "dailyAvailableHours": request.dailyAvailableHours,
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

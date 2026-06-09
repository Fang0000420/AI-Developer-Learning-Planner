import json
import re

import httpx
from pydantic import ValidationError

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

SKILL_GAP_ANALYZER_PROMPT = """
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


class SkillGapAnalyzerError(RuntimeError):
    pass


def analyze_skill_gap(request: SkillGapAnalyzeRequest) -> SkillGapAnalyzeResponse:
    if DEEPSEEK_API_KEY:
        try:
            return analyze_skill_gap_with_model(request)
        except (httpx.HTTPError, KeyError, TypeError, ValueError, ValidationError) as exc:
            raise SkillGapAnalyzerError(
                "Skill gap analyzer model response was invalid."
            ) from exc

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
                {"role": "system", "content": SKILL_GAP_ANALYZER_PROMPT},
                {
                    "role": "user",
                    "content": json.dumps(
                        {
                            "mainGoal": request.mainGoal,
                            "currentSkills": request.currentSkills,
                            "strengths": request.strengths,
                            "weaknesses": request.weaknesses,
                            "subGoals": [item.model_dump() for item in request.subGoals],
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
    return SkillGapAnalyzeResponse.model_validate(_load_json_object(content))


def analyze_skill_gap_with_mock(
    request: SkillGapAnalyzeRequest,
) -> SkillGapAnalyzeResponse:
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
        if not match:
            raise
        parsed = json.loads(match.group(0))

    if not isinstance(parsed, dict):
        raise ValueError("Skill gap analyzer response must be a JSON object.")

    return parsed

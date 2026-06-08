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
from app.schemas.goal import GoalDecomposeRequest, GoalDecomposeResponse, SubGoal

GOAL_DECOMPOSER_PROMPT = """
You are the Goal Decomposer for AI Developer Learning Planner.

Decompose the learner's main goal into practical sub-goals for a developer
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
""".strip()


class GoalDecomposerError(RuntimeError):
    pass


def decompose_goal(request: GoalDecomposeRequest) -> GoalDecomposeResponse:
    if DEEPSEEK_API_KEY:
        try:
            return decompose_goal_with_model(request)
        except (httpx.HTTPError, KeyError, TypeError, ValueError, ValidationError) as exc:
            raise GoalDecomposerError("Goal decomposer model response was invalid.") from exc

    return decompose_goal_with_mock(request)


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
                {"role": "system", "content": GOAL_DECOMPOSER_PROMPT},
                {
                    "role": "user",
                    "content": json.dumps(
                        {
                            "mainGoal": request.mainGoal,
                            "background": request.background,
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
    return GoalDecomposeResponse.model_validate(_load_json_object(content))


def decompose_goal_with_mock(request: GoalDecomposeRequest) -> GoalDecomposeResponse:
    return GoalDecomposeResponse(
        subGoals=[
            SubGoal(
                title="Clarify the target capability map",
                description=(
                    "Turn the main goal into required engineering capabilities, "
                    "expected deliverables, and measurable checkpoints."
                ),
                priority="high",
            ),
            SubGoal(
                title="Build the core agent service interface",
                description=(
                    "Implement structured FastAPI agent endpoints with validated "
                    "request and response schemas."
                ),
                priority="high",
            ),
            SubGoal(
                title="Persist agent outputs in the backend",
                description=(
                    "Connect the Java backend to the agent service and save each "
                    "successful or failed run for auditing."
                ),
                priority="high",
            ),
            SubGoal(
                title="Expose the planning result in the frontend",
                description=(
                    "Display generated planning artifacts on the goal detail page "
                    "with loading, retry, success, and error states."
                ),
                priority="medium",
            ),
            SubGoal(
                title="Verify the end-to-end MVP path",
                description=(
                    "Run focused checks from agent endpoint to backend persistence "
                    f"and UI rendering for the goal: {request.mainGoal}."
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
        if not match:
            raise
        parsed = json.loads(match.group(0))

    if not isinstance(parsed, dict):
        raise ValueError("Goal decomposer response must be a JSON object.")

    return parsed

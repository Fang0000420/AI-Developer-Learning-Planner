from fastapi import APIRouter, HTTPException, Response

from app.schemas.goal import GoalDecomposeRequest, GoalDecomposeResponse
from app.services.agent_execution import AGENT_RESPONSE_SOURCE_HEADER
from app.services.goal_decomposer import GoalDecomposerError, decompose_goal

router = APIRouter(prefix="/agent/goal", tags=["goal"])


@router.post("/decompose", response_model=GoalDecomposeResponse)
def decompose_learning_goal(
    request: GoalDecomposeRequest,
    response: Response,
) -> GoalDecomposeResponse:
    try:
        result = decompose_goal(request)
        response.headers[AGENT_RESPONSE_SOURCE_HEADER] = result.source.value
        return result.payload
    except GoalDecomposerError as exc:
        raise HTTPException(status_code=502, detail=str(exc)) from exc

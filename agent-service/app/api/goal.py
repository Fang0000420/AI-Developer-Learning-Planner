from fastapi import APIRouter, HTTPException

from app.schemas.goal import GoalDecomposeRequest, GoalDecomposeResponse
from app.services.goal_decomposer import GoalDecomposerError, decompose_goal

router = APIRouter(prefix="/agent/goal", tags=["goal"])


@router.post("/decompose", response_model=GoalDecomposeResponse)
def decompose_learning_goal(request: GoalDecomposeRequest) -> GoalDecomposeResponse:
    try:
        return decompose_goal(request)
    except GoalDecomposerError as exc:
        raise HTTPException(status_code=502, detail=str(exc)) from exc

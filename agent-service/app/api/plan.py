from fastapi import APIRouter, HTTPException

from app.schemas.plan import PlanGenerateRequest, PlanGenerateResponse
from app.services.plan_generator import PlanGeneratorError, generate_plan

router = APIRouter(prefix="/agent/plan", tags=["plan"])


@router.post("/generate", response_model=PlanGenerateResponse)
def generate_learning_plan(request: PlanGenerateRequest) -> PlanGenerateResponse:
    try:
        return generate_plan(request)
    except PlanGeneratorError as exc:
        raise HTTPException(status_code=502, detail=str(exc)) from exc

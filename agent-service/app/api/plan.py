from fastapi import APIRouter, HTTPException

from app.schemas.plan import (
    PlanAdjustRequest,
    PlanAdjustResponse,
    PlanGenerateRequest,
    PlanGenerateResponse,
)
from app.services.plan_adjuster import PlanAdjusterError, adjust_plan
from app.services.plan_generator import PlanGeneratorError, generate_plan

router = APIRouter(prefix="/agent/plan", tags=["plan"])


@router.post("/generate", response_model=PlanGenerateResponse)
def generate_learning_plan(request: PlanGenerateRequest) -> PlanGenerateResponse:
    try:
        return generate_plan(request)
    except PlanGeneratorError as exc:
        raise HTTPException(status_code=502, detail=str(exc)) from exc


@router.post("/adjust", response_model=PlanAdjustResponse)
def adjust_learning_plan(request: PlanAdjustRequest) -> PlanAdjustResponse:
    try:
        return adjust_plan(request)
    except PlanAdjusterError as exc:
        raise HTTPException(status_code=502, detail=str(exc)) from exc

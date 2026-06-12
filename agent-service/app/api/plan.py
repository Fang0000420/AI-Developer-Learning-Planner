from fastapi import APIRouter, HTTPException, Response

from app.schemas.plan import (
    PlanAdjustRequest,
    PlanAdjustResponse,
    PlanGenerateRequest,
    PlanGenerateResponse,
)
from app.services.agent_execution import AGENT_RESPONSE_SOURCE_HEADER
from app.services.plan_adjuster import PlanAdjusterError, adjust_plan
from app.services.plan_generator import PlanGeneratorError, generate_plan

router = APIRouter(prefix="/agent/plan", tags=["plan"])


@router.post("/generate", response_model=PlanGenerateResponse)
def generate_learning_plan(
    request: PlanGenerateRequest,
    response: Response,
) -> PlanGenerateResponse:
    try:
        result = generate_plan(request)
        response.headers[AGENT_RESPONSE_SOURCE_HEADER] = result.source.value
        return result.payload
    except PlanGeneratorError as exc:
        raise HTTPException(status_code=502, detail=str(exc)) from exc


@router.post("/adjust", response_model=PlanAdjustResponse)
def adjust_learning_plan(
    request: PlanAdjustRequest,
    response: Response,
) -> PlanAdjustResponse:
    try:
        result = adjust_plan(request)
        response.headers[AGENT_RESPONSE_SOURCE_HEADER] = result.source.value
        return result.payload
    except PlanAdjusterError as exc:
        raise HTTPException(status_code=502, detail=str(exc)) from exc

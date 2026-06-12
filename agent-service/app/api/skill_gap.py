from fastapi import APIRouter, HTTPException, Response

from app.schemas.skill_gap import SkillGapAnalyzeRequest, SkillGapAnalyzeResponse
from app.services.agent_execution import AGENT_RESPONSE_SOURCE_HEADER
from app.services.skill_gap_analyzer import SkillGapAnalyzerError, analyze_skill_gap

router = APIRouter(prefix="/agent/skill-gap", tags=["skill-gap"])


@router.post("/analyze", response_model=SkillGapAnalyzeResponse)
def analyze_learning_skill_gap(
    request: SkillGapAnalyzeRequest,
    response: Response,
) -> SkillGapAnalyzeResponse:
    try:
        result = analyze_skill_gap(request)
        response.headers[AGENT_RESPONSE_SOURCE_HEADER] = result.source.value
        return result.payload
    except SkillGapAnalyzerError as exc:
        raise HTTPException(status_code=502, detail=str(exc)) from exc

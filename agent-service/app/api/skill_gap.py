from fastapi import APIRouter, HTTPException

from app.schemas.skill_gap import SkillGapAnalyzeRequest, SkillGapAnalyzeResponse
from app.services.skill_gap_analyzer import SkillGapAnalyzerError, analyze_skill_gap

router = APIRouter(prefix="/agent/skill-gap", tags=["skill-gap"])


@router.post("/analyze", response_model=SkillGapAnalyzeResponse)
def analyze_learning_skill_gap(
    request: SkillGapAnalyzeRequest,
) -> SkillGapAnalyzeResponse:
    try:
        return analyze_skill_gap(request)
    except SkillGapAnalyzerError as exc:
        raise HTTPException(status_code=502, detail=str(exc)) from exc

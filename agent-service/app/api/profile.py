from fastapi import APIRouter, HTTPException

from app.schemas.profile import ProfileAnalyzeRequest, ProfileAnalyzeResponse
from app.services.profile_analyzer import ProfileAnalyzerError, analyze_profile

router = APIRouter(prefix="/agent/profile", tags=["profile"])


@router.post("/analyze", response_model=ProfileAnalyzeResponse)
def analyze_user_profile(request: ProfileAnalyzeRequest) -> ProfileAnalyzeResponse:
    try:
        return analyze_profile(request)
    except ProfileAnalyzerError as exc:
        raise HTTPException(status_code=502, detail=str(exc)) from exc

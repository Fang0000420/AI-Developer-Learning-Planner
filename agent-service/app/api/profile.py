from fastapi import APIRouter

from app.schemas.profile import ProfileAnalyzeRequest, ProfileAnalyzeResponse
from app.services.profile_analyzer import analyze_profile

router = APIRouter(prefix="/agent/profile", tags=["profile"])


@router.post("/analyze", response_model=ProfileAnalyzeResponse)
def analyze_user_profile(request: ProfileAnalyzeRequest) -> ProfileAnalyzeResponse:
    return analyze_profile(request)

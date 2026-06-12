from fastapi import APIRouter, HTTPException, Response

from app.schemas.profile import ProfileAnalyzeRequest, ProfileAnalyzeResponse
from app.services.agent_execution import AGENT_RESPONSE_SOURCE_HEADER
from app.services.profile_analyzer import ProfileAnalyzerError, analyze_profile

router = APIRouter(prefix="/agent/profile", tags=["profile"])


@router.post("/analyze", response_model=ProfileAnalyzeResponse)
def analyze_user_profile(
    request: ProfileAnalyzeRequest,
    response: Response,
) -> ProfileAnalyzeResponse:
    try:
        result = analyze_profile(request)
        response.headers[AGENT_RESPONSE_SOURCE_HEADER] = result.source.value
        return result.payload
    except ProfileAnalyzerError as exc:
        raise HTTPException(status_code=502, detail=str(exc)) from exc

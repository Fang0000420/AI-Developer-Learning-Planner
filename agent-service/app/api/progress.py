from fastapi import APIRouter, HTTPException, Response

from app.schemas.progress import ProgressReviewRequest, ProgressReviewResponse
from app.services.agent_execution import AGENT_RESPONSE_SOURCE_HEADER
from app.services.progress_reviewer import ProgressReviewerError, review_progress

router = APIRouter(prefix="/agent/progress", tags=["progress"])


@router.post("/review", response_model=ProgressReviewResponse)
def review_learning_progress(
    request: ProgressReviewRequest,
    response: Response,
) -> ProgressReviewResponse:
    try:
        result = review_progress(request)
        response.headers[AGENT_RESPONSE_SOURCE_HEADER] = result.source.value
        return result.payload
    except ProgressReviewerError as exc:
        raise HTTPException(status_code=502, detail=str(exc)) from exc

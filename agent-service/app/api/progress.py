from fastapi import APIRouter, HTTPException

from app.schemas.progress import ProgressReviewRequest, ProgressReviewResponse
from app.services.progress_reviewer import ProgressReviewerError, review_progress

router = APIRouter(prefix="/agent/progress", tags=["progress"])


@router.post("/review", response_model=ProgressReviewResponse)
def review_daily_progress(request: ProgressReviewRequest) -> ProgressReviewResponse:
    try:
        return review_progress(request)
    except ProgressReviewerError as exc:
        raise HTTPException(status_code=502, detail=str(exc)) from exc

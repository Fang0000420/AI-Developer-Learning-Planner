from fastapi import APIRouter, HTTPException

from app.schemas.project import ProjectRecommendRequest, ProjectRecommendResponse
from app.services.project_recommender import (
    ProjectRecommenderError,
    recommend_project,
)

router = APIRouter(prefix="/agent/project", tags=["project"])


@router.post("/recommend", response_model=ProjectRecommendResponse)
def recommend_learning_project(
    request: ProjectRecommendRequest,
) -> ProjectRecommendResponse:
    try:
        return recommend_project(request)
    except ProjectRecommenderError as exc:
        raise HTTPException(status_code=502, detail=str(exc)) from exc

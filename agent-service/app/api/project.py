from fastapi import APIRouter, HTTPException, Response

from app.schemas.project import ProjectRecommendRequest, ProjectRecommendResponse
from app.services.agent_execution import AGENT_RESPONSE_SOURCE_HEADER
from app.services.project_recommender import (
    ProjectRecommenderError,
    recommend_project,
)

router = APIRouter(prefix="/agent/project", tags=["project"])


@router.post("/recommend", response_model=ProjectRecommendResponse)
def recommend_learning_project(
    request: ProjectRecommendRequest,
    response: Response,
) -> ProjectRecommendResponse:
    try:
        result = recommend_project(request)
        response.headers[AGENT_RESPONSE_SOURCE_HEADER] = result.source.value
        return result.payload
    except ProjectRecommenderError as exc:
        raise HTTPException(status_code=502, detail=str(exc)) from exc

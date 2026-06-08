from fastapi import APIRouter

from app.config import APP_NAME, APP_VERSION, HEALTH_STATUS
from app.schemas.health import HealthResponse

router = APIRouter()


@router.get("/health", response_model=HealthResponse)
def get_health() -> HealthResponse:
    return HealthResponse(service=APP_NAME, status=HEALTH_STATUS, version=APP_VERSION)

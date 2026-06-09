from fastapi import APIRouter

from app.api.goal import router as goal_router
from app.api.health import router as health_router
from app.api.profile import router as profile_router
from app.api.skill_gap import router as skill_gap_router

api_router = APIRouter()
api_router.include_router(goal_router)
api_router.include_router(health_router)
api_router.include_router(profile_router)
api_router.include_router(skill_gap_router)

__all__ = ["api_router"]

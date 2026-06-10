from pydantic import BaseModel, Field

from app.services.language import ResponseLanguage


class ProfileAnalyzeRequest(BaseModel):
    background: str = Field(min_length=1)
    goal: str = Field(min_length=1)
    dailyAvailableHours: float = Field(gt=0)
    responseLanguage: ResponseLanguage = "zh"


class ProfileAnalyzeResponse(BaseModel):
    currentSkills: list[str]
    strengths: list[str]
    weaknesses: list[str]
    recommendedDirection: str

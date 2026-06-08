from pydantic import BaseModel, Field


class ProfileAnalyzeRequest(BaseModel):
    background: str = Field(min_length=1)
    goal: str = Field(min_length=1)
    dailyAvailableHours: float = Field(gt=0)


class ProfileAnalyzeResponse(BaseModel):
    currentSkills: list[str]
    strengths: list[str]
    weaknesses: list[str]
    recommendedDirection: str

from pydantic import BaseModel, Field

from app.services.language import ResponseLanguage


class GoalDecomposeRequest(BaseModel):
    mainGoal: str = Field(min_length=1)
    background: str | None = None
    responseLanguage: ResponseLanguage = "zh"


class SubGoal(BaseModel):
    title: str = Field(min_length=1)
    description: str = Field(min_length=1)
    priority: str = Field(pattern="^(high|medium|low)$")


class GoalDecomposeResponse(BaseModel):
    subGoals: list[SubGoal] = Field(min_length=1)

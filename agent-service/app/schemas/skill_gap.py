from pydantic import BaseModel, Field

from app.schemas.goal import SubGoal
from app.services.language import ResponseLanguage


class SkillGapAnalyzeRequest(BaseModel):
    mainGoal: str = Field(min_length=1)
    currentSkills: list[str] = Field(default_factory=list)
    strengths: list[str] = Field(default_factory=list)
    weaknesses: list[str] = Field(default_factory=list)
    subGoals: list[SubGoal] = Field(default_factory=list)
    knowledgeContext: str = ""
    responseLanguage: ResponseLanguage = "zh"


class SkillGap(BaseModel):
    skill: str = Field(min_length=1)
    currentLevel: str = Field(min_length=1)
    targetLevel: str = Field(min_length=1)
    priority: str = Field(pattern="^(high|medium|low)$")
    reason: str = Field(min_length=1)


class SkillGapAnalyzeResponse(BaseModel):
    skillGaps: list[SkillGap] = Field(min_length=4)

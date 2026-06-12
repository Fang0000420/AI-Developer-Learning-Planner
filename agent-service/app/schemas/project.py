from pydantic import BaseModel, Field

from app.schemas.goal import SubGoal
from app.schemas.skill_gap import SkillGap
from app.services.language import ResponseLanguage


class ProjectRecommendRequest(BaseModel):
    mainGoal: str = Field(min_length=1)
    currentSkills: list[str] = Field(default_factory=list)
    strengths: list[str] = Field(default_factory=list)
    weaknesses: list[str] = Field(default_factory=list)
    subGoals: list[SubGoal] = Field(default_factory=list)
    skillGaps: list[SkillGap] = Field(default_factory=list)
    durationDays: int = Field(gt=0)
    dailyAvailableHours: float | None = Field(default=None, ge=0)
    responseLanguage: ResponseLanguage = "zh"


class ProjectRecommendationMemory(BaseModel):
    learnerSummary: list[str] = Field(default_factory=list)
    constraints: list[str] = Field(default_factory=list)
    opportunities: list[str] = Field(default_factory=list)
    difficultySignals: list[str] = Field(default_factory=list)


class ProjectCandidate(BaseModel):
    name: str = Field(min_length=1)
    fit: str = Field(min_length=1)
    risk: str = Field(min_length=1)
    reason: str = Field(min_length=1)


class ProjectCandidateComparison(BaseModel):
    candidates: list[ProjectCandidate] = Field(min_length=1, max_length=3)
    decisionHints: list[str] = Field(default_factory=list)


class ProjectRecommendResponse(BaseModel):
    recommendedProject: str = Field(min_length=1)
    reason: str = Field(min_length=1)
    difficulty: str = Field(min_length=1)
    durationDays: int = Field(gt=0)
    dailyTimeHours: float = Field(gt=0)
    coreTechStack: list[str] = Field(min_length=1)
    finalDeliverables: list[str] = Field(min_length=1)

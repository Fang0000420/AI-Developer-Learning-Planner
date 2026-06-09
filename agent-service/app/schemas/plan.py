from pydantic import BaseModel, Field, field_validator, model_validator

from app.schemas.goal import SubGoal
from app.schemas.skill_gap import SkillGap


class PlanTask(BaseModel):
    title: str = Field(min_length=1)
    description: str = Field(min_length=1)
    estimatedMinutes: int = Field(gt=0)
    type: str = Field(min_length=1)
    deliverable: str = Field(min_length=1)
    priority: str = Field(min_length=1)

    @field_validator("priority")
    @classmethod
    def normalize_priority(cls, value: str) -> str:
        normalized = value.strip().lower()
        if normalized in {"urgent", "critical", "p0", "p1"}:
            return "high"
        if normalized in {"normal", "p2"}:
            return "medium"
        if normalized in {"optional", "p3"}:
            return "low"
        if normalized not in {"high", "medium", "low"}:
            return "medium"
        return normalized


class PlanDay(BaseModel):
    dayIndex: int = Field(gt=0)
    theme: str = Field(min_length=1)
    tasks: list[PlanTask] = Field(min_length=1)


class PlanGenerateRequest(BaseModel):
    mainGoal: str = Field(min_length=1)
    currentSkills: list[str] = Field(default_factory=list)
    strengths: list[str] = Field(default_factory=list)
    weaknesses: list[str] = Field(default_factory=list)
    subGoals: list[SubGoal] = Field(default_factory=list)
    skillGaps: list[SkillGap] = Field(default_factory=list)
    recommendedProject: str = Field(min_length=1)
    projectReason: str | None = None
    difficulty: str | None = None
    coreTechStack: list[str] = Field(default_factory=list)
    finalDeliverables: list[str] = Field(default_factory=list)
    durationDays: int = Field(gt=0)
    dailyAvailableHours: float | None = Field(default=None, ge=0)

    @field_validator("durationDays")
    @classmethod
    def validate_supported_duration(cls, value: int) -> int:
        if value not in {14, 21}:
            raise ValueError("durationDays must be 14 or 21.")
        return value


class PlanGenerateResponse(BaseModel):
    planTitle: str = Field(min_length=1)
    durationDays: int = Field(gt=0)
    days: list[PlanDay] = Field(min_length=1)

    @model_validator(mode="after")
    def validate_day_count(self) -> "PlanGenerateResponse":
        if len(self.days) != self.durationDays:
            raise ValueError("days length must match durationDays.")
        return self

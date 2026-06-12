from typing import Literal

from pydantic import BaseModel, Field, field_validator, model_validator

from app.schemas.goal import SubGoal
from app.schemas.skill_gap import SkillGap
from app.services.language import ResponseLanguage


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


class PlanGenerationMemory(BaseModel):
    completedDayIndexes: list[int] = Field(default_factory=list)
    establishedThemes: list[str] = Field(default_factory=list)
    carryForwardConstraints: list[str] = Field(default_factory=list)
    nextFocusHints: list[str] = Field(default_factory=list)


class PlanGenerateChunkResponse(BaseModel):
    planTitle: str = Field(min_length=1)
    days: list[PlanDay] = Field(min_length=1)
    memory: PlanGenerationMemory = Field(default_factory=PlanGenerationMemory)

    @model_validator(mode="after")
    def validate_chunk(self) -> "PlanGenerateChunkResponse":
        if len(self.days) > 2:
            raise ValueError("chunk days length must not exceed 2.")
        day_indexes = [day.dayIndex for day in self.days]
        if len(set(day_indexes)) != len(day_indexes):
            raise ValueError("chunk dayIndex values must be unique.")
        return self


class PlanGenerateRequest(BaseModel):
    mainGoal: str = Field(min_length=1)
    currentSkills: list[str] = Field(default_factory=list)
    strengths: list[str] = Field(default_factory=list)
    weaknesses: list[str] = Field(default_factory=list)
    subGoals: list[SubGoal] = Field(default_factory=list)
    skillGaps: list[SkillGap] = Field(default_factory=list)
    recommendedProject: str | None = None
    projectReason: str | None = None
    difficulty: str | None = None
    coreTechStack: list[str] = Field(default_factory=list)
    finalDeliverables: list[str] = Field(default_factory=list)
    durationDays: int = Field(gt=0)
    dailyAvailableHours: float | None = Field(default=None, ge=0)
    responseLanguage: ResponseLanguage = "zh"

    @field_validator("durationDays")
    @classmethod
    def validate_supported_duration(cls, value: int) -> int:
        if value < 7 or value > 60:
            raise ValueError("durationDays must be between 7 and 60.")
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


class PlanAdjustTask(BaseModel):
    id: int | None = Field(default=None, gt=0)
    dayIndex: int | None = Field(default=None, gt=0)
    taskOrder: int | None = Field(default=None, gt=0)
    title: str = Field(min_length=1)
    description: str = Field(min_length=1)
    estimatedMinutes: int = Field(gt=0)
    type: str = Field(min_length=1)
    deliverable: str = Field(min_length=1)
    priority: str = Field(min_length=1)
    status: str | None = None

    @field_validator("priority")
    @classmethod
    def normalize_adjust_priority(cls, value: str) -> str:
        return PlanTask.normalize_priority(value)


class PlanAdjustDay(BaseModel):
    dayIndex: int = Field(gt=0)
    theme: str = Field(min_length=1)
    tasks: list[PlanAdjustTask] = Field(default_factory=list)


class PlanAdjustReview(BaseModel):
    completedTasks: list[str] = Field(default_factory=list)
    unfinishedTasks: list[str] = Field(default_factory=list)
    blockers: list[str] = Field(default_factory=list)
    impact: Literal["none", "minor", "medium", "major"]
    suggestion: str = Field(min_length=1)


class PlanAdjustRequest(BaseModel):
    planId: int = Field(gt=0)
    currentDayIndex: int = Field(gt=0)
    currentPlan: list[PlanAdjustDay] = Field(default_factory=list)
    todayTasks: list[PlanAdjustTask] = Field(min_length=1)
    progressReview: PlanAdjustReview
    unfinishedTasks: list[PlanAdjustTask] = Field(default_factory=list)
    nextDayTasks: list[PlanAdjustTask] = Field(default_factory=list)
    responseLanguage: ResponseLanguage = "zh"


class PlanMovedTask(BaseModel):
    taskId: int | None = Field(default=None, gt=0)
    title: str = Field(min_length=1)
    fromDayIndex: int = Field(gt=0)
    toDayIndex: int = Field(gt=0)
    reason: str = Field(min_length=1)


class PlanSplitTask(BaseModel):
    sourceTaskId: int | None = Field(default=None, gt=0)
    sourceTitle: str = Field(min_length=1)
    parts: list[PlanAdjustTask] = Field(min_length=1)
    reason: str = Field(min_length=1)


class PlanAdjustResponse(BaseModel):
    nextDayTasks: list[PlanAdjustTask] = Field(default_factory=list)
    movedTasks: list[PlanMovedTask] = Field(default_factory=list)
    splitTasks: list[PlanSplitTask] = Field(default_factory=list)
    reason: str = Field(min_length=1)

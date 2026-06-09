from typing import Literal

from pydantic import BaseModel, Field, field_validator


class ProgressTask(BaseModel):
    id: int = Field(gt=0)
    title: str = Field(min_length=1)
    description: str | None = None
    estimatedMinutes: int | None = Field(default=None, gt=0)
    type: str | None = None
    deliverable: str | None = None
    priority: str | None = None


class ProgressReviewRequest(BaseModel):
    dayIndex: int = Field(gt=0)
    todayTasks: list[ProgressTask] = Field(min_length=1)
    userFeedback: str = Field(min_length=1)
    completedTasks: list[ProgressTask] = Field(default_factory=list)
    unfinishedTasks: list[ProgressTask] = Field(default_factory=list)
    blockers: list[str] = Field(default_factory=list)

    @field_validator("userFeedback")
    @classmethod
    def trim_feedback(cls, value: str) -> str:
        return value.strip()

    @field_validator("blockers")
    @classmethod
    def clean_blockers(cls, values: list[str]) -> list[str]:
        return [value.strip() for value in values if value and value.strip()]


class ProgressReviewResponse(BaseModel):
    completedTasks: list[str] = Field(default_factory=list)
    unfinishedTasks: list[str] = Field(default_factory=list)
    blockers: list[str] = Field(default_factory=list)
    impact: Literal["none", "minor", "medium", "major"]
    suggestion: str = Field(min_length=1)

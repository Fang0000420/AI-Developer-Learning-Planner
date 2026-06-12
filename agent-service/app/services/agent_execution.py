from dataclasses import dataclass
from enum import Enum
from typing import Generic, TypeVar

T = TypeVar("T")

AGENT_RESPONSE_SOURCE_HEADER = "X-Agent-Response-Source"


class AgentResponseSource(str, Enum):
    MODEL = "model"
    FALLBACK = "fallback"


@dataclass(frozen=True)
class AgentExecutionResult(Generic[T]):
    payload: T
    source: AgentResponseSource

    def __getattr__(self, name: str):
        return getattr(self.payload, name)

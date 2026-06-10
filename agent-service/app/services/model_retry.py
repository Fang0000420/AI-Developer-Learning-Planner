import logging
from collections.abc import Callable
from typing import TypeVar

import httpx
from pydantic import ValidationError

from app.observability import get_request_id

T = TypeVar("T")

RETRYABLE_MODEL_ERRORS = (
    httpx.HTTPError,
    KeyError,
    TypeError,
    ValueError,
    ValidationError,
)


def retry_model_call(call: Callable[[], T], attempts: int = 3) -> T:
    logger = logging.getLogger("app.model_retry")
    last_error: Exception | None = None
    total_attempts = max(1, attempts)
    for attempt in range(1, total_attempts + 1):
        try:
            return call()
        except RETRYABLE_MODEL_ERRORS as exc:
            last_error = exc
            logger.warning(
                "model call attempt failed attempt=%s attempts=%s error=%s requestId=%s",
                attempt,
                total_attempts,
                str(exc),
                get_request_id() or "none",
            )

    if last_error is None:
        raise RuntimeError("Model call failed without an error.")
    raise last_error

import logging
from collections.abc import Callable
from typing import TypeVar

import httpx
from pydantic import ValidationError

from app.observability import get_request_id

T = TypeVar("T")

MODEL_PAYLOAD_ERRORS = (
    KeyError,
    TypeError,
    ValueError,
    ValidationError,
)
MODEL_CALL_ERRORS = (httpx.HTTPError, *MODEL_PAYLOAD_ERRORS)


class ModelCallNonRetryableError(RuntimeError):
    pass


class ModelCallRetryExhaustedError(RuntimeError):
    pass


def retry_model_call(call: Callable[[], T], attempts: int = 3) -> T:
    logger = logging.getLogger("app.model_retry")
    last_error: Exception | None = None
    total_attempts = max(1, attempts)
    for attempt in range(1, total_attempts + 1):
        try:
            return call()
        except MODEL_CALL_ERRORS as exc:
            if not _is_retryable_model_error(exc):
                logger.error(
                    "model call failed with non-retryable error error=%s requestId=%s",
                    _describe_model_error(exc),
                    get_request_id() or "none",
                )
                raise ModelCallNonRetryableError(_describe_model_error(exc)) from exc

            last_error = exc
            logger.warning(
                "model call retryable attempt failed attempt=%s attempts=%s error=%s requestId=%s",
                attempt,
                total_attempts,
                _describe_model_error(exc),
                get_request_id() or "none",
            )

    if last_error is None:
        raise RuntimeError("Model call failed without an error.")
    raise ModelCallRetryExhaustedError(_describe_model_error(last_error)) from last_error


def _is_retryable_model_error(exc: Exception) -> bool:
    if isinstance(exc, httpx.HTTPStatusError):
        status_code = exc.response.status_code
        return status_code in {408, 409, 425, 429} or status_code >= 500

    if isinstance(exc, httpx.HTTPError):
        return True

    return isinstance(exc, MODEL_PAYLOAD_ERRORS)


def _describe_model_error(exc: Exception) -> str:
    if isinstance(exc, httpx.HTTPStatusError):
        return f"HTTP {exc.response.status_code}: {exc.response.text}"
    return str(exc) or exc.__class__.__name__

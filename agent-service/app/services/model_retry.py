from collections.abc import Callable
from typing import TypeVar

import httpx
from pydantic import ValidationError

T = TypeVar("T")

RETRYABLE_MODEL_ERRORS = (
    httpx.HTTPError,
    KeyError,
    TypeError,
    ValueError,
    ValidationError,
)


def retry_model_call(call: Callable[[], T], attempts: int = 3) -> T:
    last_error: Exception | None = None
    for _ in range(max(1, attempts)):
        try:
            return call()
        except RETRYABLE_MODEL_ERRORS as exc:
            last_error = exc

    if last_error is None:
        raise RuntimeError("Model call failed without an error.")
    raise last_error

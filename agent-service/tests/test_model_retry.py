import httpx
import pytest

from app.services.model_retry import (
    ModelCallNonRetryableError,
    ModelCallRetryExhaustedError,
    retry_model_call,
)


def test_retry_model_call_returns_after_retryable_failure():
    attempts = {"count": 0}

    def flaky_call():
        attempts["count"] += 1
        if attempts["count"] < 3:
            raise ValueError("invalid model output")
        return "ok"

    assert retry_model_call(flaky_call) == "ok"
    assert attempts["count"] == 3


def test_retry_model_call_raises_last_retryable_failure():
    def failing_call():
        raise ValueError("still invalid")

    with pytest.raises(ModelCallRetryExhaustedError, match="still invalid"):
        retry_model_call(failing_call, attempts=2)


def test_retry_model_call_does_not_retry_non_retryable_http_status():
    attempts = {"count": 0}

    def unauthorized_call():
        attempts["count"] += 1
        request = httpx.Request("POST", "https://api.example.test/chat/completions")
        response = httpx.Response(401, request=request, text="invalid api key")
        raise httpx.HTTPStatusError("unauthorized", request=request, response=response)

    with pytest.raises(ModelCallNonRetryableError, match="HTTP 401"):
        retry_model_call(unauthorized_call, attempts=3)

    assert attempts["count"] == 1

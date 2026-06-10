import pytest

from app.services.model_retry import retry_model_call


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

    with pytest.raises(ValueError, match="still invalid"):
        retry_model_call(failing_call, attempts=2)

import logging
import time
import uuid
from contextvars import ContextVar

from fastapi import Request, Response

REQUEST_ID_HEADER = "X-Request-Id"

_request_id: ContextVar[str | None] = ContextVar("request_id", default=None)


def get_request_id() -> str | None:
    return _request_id.get()


async def request_logging_middleware(request: Request, call_next) -> Response:
    logger = logging.getLogger("app.request")
    request_id = request.headers.get(REQUEST_ID_HEADER) or str(uuid.uuid4())
    token = _request_id.set(request_id)
    started_at = time.perf_counter()

    try:
        response = await call_next(request)
        response.headers[REQUEST_ID_HEADER] = request_id
        logger.info(
            "request completed method=%s path=%s status=%s latencyMs=%s requestId=%s",
            request.method,
            request.url.path,
            response.status_code,
            elapsed_ms(started_at),
            request_id,
        )
        return response
    except Exception:
        logger.exception(
            "request failed method=%s path=%s latencyMs=%s requestId=%s",
            request.method,
            request.url.path,
            elapsed_ms(started_at),
            request_id,
        )
        raise
    finally:
        _request_id.reset(token)


def elapsed_ms(started_at: float) -> int:
    return max(1, int((time.perf_counter() - started_at) * 1000))

import hashlib
import json
import logging
from typing import Any

from redis import Redis
from redis.exceptions import RedisError

from app.config import REDIS_CACHE_TTL_SECONDS, REDIS_URL

log = logging.getLogger(__name__)

_redis_client: Redis | None = None
_redis_unavailable = False


def cache_get(namespace: str, payload: dict[str, Any]) -> dict[str, Any] | None:
    client = _get_client()
    if client is None:
        return None
    try:
        raw = client.get(_cache_key(namespace, payload))
        if raw is None:
            return None
        parsed = json.loads(raw)
        return parsed if isinstance(parsed, dict) else None
    except (RedisError, json.JSONDecodeError) as exc:
        log.debug("Redis cache read failed for %s: %s", namespace, exc)
        return None


def cache_set(namespace: str, payload: dict[str, Any], value: dict[str, Any]) -> None:
    client = _get_client()
    if client is None:
        return
    try:
        client.setex(
            _cache_key(namespace, payload),
            REDIS_CACHE_TTL_SECONDS,
            json.dumps(value, ensure_ascii=False, sort_keys=True),
        )
    except (RedisError, TypeError) as exc:
        log.debug("Redis cache write failed for %s: %s", namespace, exc)


def _cache_key(namespace: str, payload: dict[str, Any]) -> str:
    serialized = json.dumps(payload, ensure_ascii=False, sort_keys=True, default=str)
    payload_hash = hashlib.sha256(serialized.encode("utf-8")).hexdigest()
    return f"agent-cache:{namespace}:{payload_hash}"


def _get_client() -> Redis | None:
    global _redis_client, _redis_unavailable
    if _redis_unavailable or not REDIS_URL:
        return None
    if _redis_client is not None:
        return _redis_client
    try:
        _redis_client = Redis.from_url(REDIS_URL, decode_responses=True)
        _redis_client.ping()
        return _redis_client
    except RedisError as exc:
        log.debug("Redis client unavailable: %s", exc)
        _redis_unavailable = True
        return None

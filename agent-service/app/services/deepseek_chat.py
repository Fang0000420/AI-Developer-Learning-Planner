import json

import httpx

from app.config import DEEPSEEK_API_BASE_URL, DEEPSEEK_API_KEY


def chat_completion_json(
    *,
    model: str,
    messages: list[dict[str, str]],
    timeout_seconds: float,
    temperature: float = 0.2,
    max_tokens: int | None = None,
    stream: bool = False,
) -> dict[str, object]:
    payload: dict[str, object] = {
        "model": model,
        "messages": messages,
        "response_format": {"type": "json_object"},
        "stream": stream,
        "temperature": temperature,
    }
    if max_tokens is not None:
        payload["max_tokens"] = max_tokens

    response = httpx.post(
        f"{DEEPSEEK_API_BASE_URL.rstrip('/')}/chat/completions",
        headers={
            "Authorization": f"Bearer {DEEPSEEK_API_KEY}",
            "Content-Type": "application/json",
        },
        json=payload,
        timeout=timeout_seconds,
    )
    response.raise_for_status()

    body = response.json()
    choice = _first_choice(body)
    finish_reason = choice.get("finish_reason")
    if finish_reason in {"length", "insufficient_system_resource"}:
        raise ValueError(f"DeepSeek completion stopped early: {finish_reason}")

    message = choice.get("message")
    if not isinstance(message, dict):
        raise ValueError("DeepSeek completion message is missing.")
    content = message.get("content")
    if not isinstance(content, str) or not content.strip():
        raise ValueError("DeepSeek completion content is empty.")

    parsed = json.loads(content)
    if not isinstance(parsed, dict):
        raise ValueError("DeepSeek completion JSON must be an object.")
    return parsed


def _first_choice(body: dict[str, object]) -> dict[str, object]:
    choices = body.get("choices")
    if not isinstance(choices, list) or not choices:
        raise ValueError("DeepSeek completion choices are missing.")
    choice = choices[0]
    if not isinstance(choice, dict):
        raise ValueError("DeepSeek completion choice is invalid.")
    return choice

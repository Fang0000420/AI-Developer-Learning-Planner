from typing import Literal

ResponseLanguage = Literal["zh", "en"]


def is_zh(language: str | None) -> bool:
    return (language or "zh").lower() == "zh"


def prompt_for(language: str | None, zh_prompt: str, en_prompt: str) -> str:
    return zh_prompt if is_zh(language) else en_prompt

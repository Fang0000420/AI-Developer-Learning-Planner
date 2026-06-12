from pytest import MonkeyPatch

from app.services import deepseek_chat
from app.services.prompt_catalog import PROMPT_CATALOG, prompt_section


def test_prompt_catalog_keeps_zh_en_keys_in_sync() -> None:
    for agent_sections in PROMPT_CATALOG.values():
        for section in agent_sections.values():
            assert set(section.keys()) == {"zh", "en"}


def test_chat_completion_json_requests_json_output(monkeypatch: MonkeyPatch) -> None:
    captured: dict[str, object] = {}

    class FakeResponse:
        def raise_for_status(self) -> None:
            return None

        def json(self) -> dict[str, object]:
            return {
                "choices": [
                    {
                        "finish_reason": "stop",
                        "message": {"content": '{"ok": true}'},
                    }
                ]
            }

    def fake_post(*args, json, **kwargs):
        captured["json"] = json
        captured["kwargs"] = kwargs
        return FakeResponse()

    monkeypatch.setattr(deepseek_chat.httpx, "post", fake_post)

    response = deepseek_chat.chat_completion_json(
        model="deepseek-v4-flash",
        messages=[{"role": "system", "content": "Return JSON only."}],
        timeout_seconds=30,
        max_tokens=100,
    )

    assert response == {"ok": True}
    assert captured["json"]["response_format"] == {"type": "json_object"}
    assert captured["json"]["model"] == "deepseek-v4-flash"
    assert captured["json"]["stream"] is False


def test_prompt_catalog_contains_all_generalized_agents() -> None:
    for agent in {
        "profile_analyzer",
        "goal_decomposer",
        "progress_reviewer",
        "plan_adjuster",
        "plan_generator",
        "project_recommender",
    }:
        assert agent in PROMPT_CATALOG
        assert "system_rules" in PROMPT_CATALOG[agent]


def test_prompt_section_returns_localized_prompt() -> None:
    zh_prompt = prompt_section("profile_analyzer", "system_rules", "zh")
    en_prompt = prompt_section("profile_analyzer", "system_rules", "en")

    assert "能力画像分析器" in zh_prompt
    assert "Profile Analyzer" in en_prompt

PROMPT_CATALOG = {
    "plan_generator": {
        "system_rules": {
            "zh": (
                "你是 AI Developer Learning Planner 的计划生成器。"
                "你会分多轮生成学习计划。只返回 JSON，不要输出 markdown 或解释。"
                "每轮只生成当前要求的天数，不得重复已完成天数。"
                "任务必须具体、可执行，并逐步推进目标能力、核心聚焦领域和可验证成果。"
            ),
            "en": (
                "You are the Plan Generator for AI Developer Learning Planner. "
                "You generate the learning plan across multiple rounds. Return JSON only "
                "with no markdown or commentary. Generate only the requested days for the "
                "current round and never rewrite completed days. Tasks must be concrete, "
                "actionable, and move the learner toward the goal capabilities, focus areas, "
                "and expected outcomes."
            ),
        },
        "global_context_instruction": {
            "zh": (
                "以下 JSON 是全局上下文，包含目标、当前技能、子目标、技能差距、推荐项目、"
                "核心聚焦领域、预期产出、计划总天数、每日可用时间和输出语言。"
                "请把它作为整个计划的唯一事实基础。"
            ),
            "en": (
                "The following JSON is the global context for the whole plan, including the goal, "
                "current skills, sub-goals, skill gaps, recommended track, focus areas, "
                "expected outcomes, total duration, daily available time, and response language. "
                "Treat it as the source of truth."
            ),
        },
        "memory_instruction": {
            "zh": (
                "如提供 previousMemory，请延续既定主题、约束、交付物和下一阶段重点。"
                "如提供 previousChunk，请只把它当作最近一轮的事实记录。"
            ),
            "en": (
                "If previousMemory is provided, continue the established themes, constraints, "
                "expected outcomes, and next-step focus. If previousChunk is provided, treat it as "
                "the factual result from the most recent round."
            ),
        },
        "round_instruction": {
            "zh": (
                "本轮只生成第 [START_DAY] 天到第 [END_DAY] 天。"
                "返回 JSON 结构必须为 "
                '{"planTitle":"string","days":[{"dayIndex":1,"theme":"string",'
                '"tasks":[{"title":"string","description":"string","estimatedMinutes":60,'
                '"type":"learn|practice|apply|review|assessment|reflection",'
                '"deliverable":"string","priority":"high|medium|low"}]}],'
                '"memory":{"completedDayIndexes":[1],"establishedThemes":["string"],'
                '"carryForwardConstraints":["string"],"nextFocusHints":["string"]}}。'
                "days 只包含本轮天数；memory 必须概括当前累计脉络。"
            ),
            "en": (
                "Generate only Day [START_DAY] through Day [END_DAY] in this round. "
                "Return JSON with exactly this shape: "
                '{"planTitle":"string","days":[{"dayIndex":1,"theme":"string",'
                '"tasks":[{"title":"string","description":"string","estimatedMinutes":60,'
                '"type":"learn|practice|apply|review|assessment|reflection",'
                '"deliverable":"string","priority":"high|medium|low"}]}],'
                '"memory":{"completedDayIndexes":[1],"establishedThemes":["string"],'
                '"carryForwardConstraints":["string"],"nextFocusHints":["string"]}}. '
                "The days array must contain only the days for this round, "
                "and memory must summarize the cumulative plan direction."
            ),
        },
    },
    "project_recommender": {
        "system_rules": {
            "zh": (
                "你是 AI Developer Learning Planner 的项目推荐器。"
                "你会分多轮形成最终推荐，但最终只推荐一个聚焦的学习主线或实践方向。"
                "始终只返回 JSON。"
            ),
            "en": (
                "You are the Project Recommender for AI Developer Learning Planner. "
                "You work across multiple rounds to form the final recommendation, "
                "but the final answer must recommend exactly one focused learning track "
                "or practice direction. Return JSON only."
            ),
        },
        "summary_instruction": {
            "zh": (
                "先根据输入画像总结学习者约束与机会点。"
                '返回 JSON：{"learnerSummary":["string"],"constraints":["string"],'
                '"opportunities":["string"],"difficultySignals":["string"]}。'
            ),
            "en": (
                "First summarize the learner's constraints and opportunities "
                "from the input profile. "
                'Return JSON: {"learnerSummary":["string"],"constraints":["string"],'
                '"opportunities":["string"],"difficultySignals":["string"]}.'
            ),
        },
        "comparison_instruction": {
            "zh": (
                "基于已有摘要，比较 2 到 3 个候选学习主线或实践方向。"
                '返回 JSON：{"candidates":[{"name":"string","fit":"string",'
                '"risk":"string","reason":"string"}],"decisionHints":["string"]}。'
            ),
            "en": (
                "Using the existing summary, compare 2 to 3 candidate learning tracks "
                "or practice directions. "
                'Return JSON: {"candidates":[{"name":"string","fit":"string",'
                '"risk":"string","reason":"string"}],"decisionHints":["string"]}.'
            ),
        },
        "final_instruction": {
            "zh": (
                "基于前两轮结果，收敛成最终推荐。"
                '返回 JSON：{"recommendedProject":"string","reason":"string",'
                '"difficulty":"string","durationDays":21,"dailyTimeHours":2,'
                '"coreTechStack":["string"],"finalDeliverables":["string"]}。'
                "recommendedProject 表示学习主线标题；当目标不是技术领域时，"
                "coreTechStack 表示核心聚焦领域，finalDeliverables 表示成果证明或预期产出。"
            ),
            "en": (
                "Use the first two rounds to converge on the final recommendation. "
                'Return JSON: {"recommendedProject":"string","reason":"string",'
                '"difficulty":"string","durationDays":21,"dailyTimeHours":2,'
                '"coreTechStack":["string"],"finalDeliverables":["string"]}. '
                "recommendedProject is the title of the learning track; "
                "when the goal is not technical, treat coreTechStack as focus areas "
                "and finalDeliverables as evidence of progress or expected outcomes."
            ),
        },
    },
}


def prompt_section(agent: str, section: str, language: str | None) -> str:
    normalized_language = "zh" if (language or "zh").lower() == "zh" else "en"
    return PROMPT_CATALOG[agent][section][normalized_language]

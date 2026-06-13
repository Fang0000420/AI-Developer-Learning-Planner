package com.aidevplanner.backend.knowledge;

import java.util.List;

public record KnowledgeContextBundle(
        String contextText,
        List<String> evidence,
        List<String> documentTitles,
        int documentCount
) {

    public static KnowledgeContextBundle empty() {
        return new KnowledgeContextBundle("", List.of(), List.of(), 0);
    }
}

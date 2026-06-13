package com.aidevplanner.backend.knowledge;

import com.aidevplanner.backend.goal.Goal;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class KnowledgeContextService {

    private static final Pattern TOKEN_SPLIT_PATTERN = Pattern.compile("[^\\p{L}\\p{N}]+");

    private final KnowledgeChunkRepository knowledgeChunkRepository;
    private final KnowledgeDocumentRepository knowledgeDocumentRepository;

    public KnowledgeContextService(
            KnowledgeChunkRepository knowledgeChunkRepository,
            KnowledgeDocumentRepository knowledgeDocumentRepository
    ) {
        this.knowledgeChunkRepository = knowledgeChunkRepository;
        this.knowledgeDocumentRepository = knowledgeDocumentRepository;
    }

    public KnowledgeContextBundle buildForGoal(Goal goal) {
        List<KnowledgeDocument> documents = knowledgeDocumentRepository
                .findByUserIdAndEnabledTrueAndStatusOrderByUpdatedAtDesc(
                        goal.getUser().getId(),
                        KnowledgeDocumentStatus.READY
                );
        if (documents.isEmpty()) {
            return KnowledgeContextBundle.empty();
        }

        Set<String> queryTokens = queryTokens(goal);
        Map<Long, KnowledgeDocument> documentById = documents.stream()
                .collect(Collectors.toMap(KnowledgeDocument::getId, item -> item));
        List<KnowledgeChunk> chunks = knowledgeChunkRepository.findByDocumentIdIn(documentById.keySet());
        if (chunks.isEmpty()) {
            return fallbackFromSummaries(goal, documents, queryTokens);
        }

        List<ScoredChunk> scoredChunks = chunks.stream()
                .map(chunk -> new ScoredChunk(
                        chunk,
                        scoreChunk(chunk, documentById.get(chunk.getDocument().getId()), queryTokens)
                ))
                .filter(item -> item.score() > 0)
                .sorted(Comparator.comparingInt(ScoredChunk::score).reversed())
                .toList();

        if (scoredChunks.isEmpty()) {
            return fallbackFromSummaries(goal, documents, queryTokens);
        }

        List<String> evidence = new ArrayList<>();
        List<String> contextSections = new ArrayList<>();
        LinkedHashSet<String> documentTitles = new LinkedHashSet<>();
        for (ScoredChunk scoredChunk : scoredChunks.stream().limit(4).toList()) {
            KnowledgeDocument document = documentById.get(scoredChunk.chunk().getDocument().getId());
            if (document == null) {
                continue;
            }
            documentTitles.add(document.getTitle());
            evidence.add(formatEvidence(goal, document.getTitle(), scoredChunk.chunk().getContent()));
            contextSections.add(formatContextSection(document, scoredChunk.chunk().getContent()));
        }

        return new KnowledgeContextBundle(
                String.join("\n\n", contextSections),
                evidence,
                List.copyOf(documentTitles),
                documentTitles.size()
        );
    }

    private KnowledgeContextBundle fallbackFromSummaries(
            Goal goal,
            List<KnowledgeDocument> documents,
            Set<String> queryTokens
    ) {
        List<KnowledgeDocument> ranked = documents.stream()
                .sorted(Comparator.comparingInt(
                        document -> -scoreText(document.getSummary(), queryTokens) - scoreText(document.getTitle(), queryTokens)
                ))
                .limit(3)
                .toList();

        List<String> evidence = ranked.stream()
                .map(document -> formatEvidence(
                        goal,
                        document.getTitle(),
                        firstPresent(document.getSummary(), document.getOriginalFileName())
                ))
                .toList();
        List<String> contextSections = ranked.stream()
                .map(document -> formatContextSection(
                        document,
                        firstPresent(document.getSummary(), document.getOriginalFileName())
                ))
                .toList();
        List<String> titles = ranked.stream().map(KnowledgeDocument::getTitle).toList();
        return new KnowledgeContextBundle(String.join("\n\n", contextSections), evidence, titles, titles.size());
    }

    private int scoreChunk(KnowledgeChunk chunk, KnowledgeDocument document, Set<String> queryTokens) {
        if (document == null) {
            return 0;
        }
        int score = 0;
        score += scoreText(document.getTitle(), queryTokens) * 4;
        score += scoreText(document.getSummary(), queryTokens) * 3;
        score += scoreText(chunk.getContent(), queryTokens) * 5;
        return score;
    }

    private int scoreText(String text, Collection<String> queryTokens) {
        if (text == null || text.isBlank() || queryTokens.isEmpty()) {
            return 0;
        }
        Set<String> textTokens = tokenize(text);
        int score = 0;
        for (String token : queryTokens) {
            if (textTokens.contains(token)) {
                score++;
            }
        }
        return score;
    }

    private Set<String> queryTokens(Goal goal) {
        Set<String> values = new LinkedHashSet<>();
        values.addAll(tokenize(goal.getTitle()));
        values.addAll(tokenize(goal.getDescription()));
        if (goal.getUser().getBackground() != null) {
            values.addAll(tokenize(goal.getUser().getBackground()));
        }
        return values;
    }

    private Set<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return Set.of();
        }
        return TOKEN_SPLIT_PATTERN.splitAsStream(text.toLowerCase(Locale.ROOT))
                .filter(token -> token != null && !token.isBlank())
                .filter(token -> token.length() >= 2)
                .limit(80)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String formatContextSection(KnowledgeDocument document, String content) {
        return "Source: " + document.getTitle() + "\n"
                + "Summary: " + firstPresent(document.getSummary(), "No summary available.") + "\n"
                + "Excerpt: " + shorten(content, 260);
    }

    private String formatEvidence(Goal goal, String title, String content) {
        if (isZh(goal)) {
            return "知识库证据《" + title + "》：" + shorten(content, 120);
        }
        return "Knowledge evidence from \"" + title + "\": " + shorten(content, 120);
    }

    private String shorten(String text, int maxLength) {
        String normalized = text == null ? "" : text.replace("\n", " ").replaceAll("\\s+", " ").trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength) + "...";
    }

    private String firstPresent(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private boolean isZh(Goal goal) {
        return goal.getResponseLanguage() != null
                && "zh".equalsIgnoreCase(goal.getResponseLanguage().name());
    }

    private record ScoredChunk(KnowledgeChunk chunk, int score) {
    }
}

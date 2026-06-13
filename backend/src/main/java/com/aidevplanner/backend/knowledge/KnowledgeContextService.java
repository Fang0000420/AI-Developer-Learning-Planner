package com.aidevplanner.backend.knowledge;

import com.aidevplanner.backend.goal.Goal;
import com.aidevplanner.backend.goal.GoalKnowledgePreference;
import com.aidevplanner.backend.goal.GoalKnowledgePreferenceResponse;
import com.aidevplanner.backend.goal.GoalKnowledgePreferenceService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class KnowledgeContextService {

    private static final Pattern TOKEN_SPLIT_PATTERN = Pattern.compile("[^\\p{L}\\p{N}]+");
    private static final int MAX_CONTEXT_SECTIONS = 4;
    private static final int MAX_DOCUMENTS = 3;
    private static final int MAX_CHUNKS_PER_DOCUMENT = 2;

    private final KnowledgeChunkRepository knowledgeChunkRepository;
    private final KnowledgeDocumentRepository knowledgeDocumentRepository;
    private final GoalKnowledgePreferenceService goalKnowledgePreferenceService;

    public KnowledgeContextService(
            KnowledgeChunkRepository knowledgeChunkRepository,
            KnowledgeDocumentRepository knowledgeDocumentRepository,
            GoalKnowledgePreferenceService goalKnowledgePreferenceService
    ) {
        this.knowledgeChunkRepository = knowledgeChunkRepository;
        this.knowledgeDocumentRepository = knowledgeDocumentRepository;
        this.goalKnowledgePreferenceService = goalKnowledgePreferenceService;
    }

    public KnowledgeContextBundle buildForGoal(Goal goal) {
        List<KnowledgeDocument> documents = readyDocuments(goal);
        if (documents.isEmpty()) {
            return KnowledgeContextBundle.empty();
        }

        GoalKnowledgePreference preference = goalKnowledgePreferenceService.read(goal);
        QueryProfile queryProfile = queryProfile(goal);
        Map<Long, KnowledgeDocument> documentById = documents.stream()
                .collect(Collectors.toMap(KnowledgeDocument::getId, item -> item));
        List<KnowledgeChunk> chunks = knowledgeChunkRepository.findByDocumentIdIn(documentById.keySet());
        if (chunks.isEmpty()) {
            return fallbackFromSummaries(goal, documents, queryProfile);
        }

        List<ScoredChunk> scoredChunks = chunks.stream()
                .map(chunk -> new ScoredChunk(
                        chunk,
                        scoreChunk(chunk, documentById.get(chunk.getDocument().getId()), queryProfile, preference)
                ))
                .filter(item -> item.score() >= 3)
                .sorted(Comparator
                        .comparingInt(ScoredChunk::score).reversed()
                        .thenComparing(item -> item.chunk().getChunkIndex()))
                .toList();

        if (scoredChunks.isEmpty()) {
            return fallbackFromSummaries(goal, documents, queryProfile);
        }

        List<ScoredChunk> selectedChunks = pickDiverseChunks(scoredChunks);
        List<String> evidence = new ArrayList<>();
        List<String> contextSections = new ArrayList<>();
        LinkedHashSet<String> documentTitles = new LinkedHashSet<>();
        for (ScoredChunk scoredChunk : selectedChunks) {
            KnowledgeDocument document = documentById.get(scoredChunk.chunk().getDocument().getId());
            if (document == null) {
                continue;
            }
            documentTitles.add(document.getTitle());
            evidence.add(formatEvidence(goal, document, scoredChunk.chunk().getContent()));
            contextSections.add(formatContextSection(document, scoredChunk.chunk().getContent(), scoredChunk.score()));
        }

        return new KnowledgeContextBundle(
                String.join("\n\n", contextSections),
                evidence,
                List.copyOf(documentTitles),
                documentTitles.size()
        );
    }

    public KnowledgeRetrievalPreviewResponse previewForGoal(Goal goal) {
        List<KnowledgeDocument> documents = readyDocuments(goal);
        if (documents.isEmpty()) {
            return new KnowledgeRetrievalPreviewResponse(
                    goal.getId(),
                    goal.getTitle(),
                    0,
                    0,
                    0,
                    previewRules(goal, goalKnowledgePreferenceService.read(goal)),
                    List.of()
            );
        }

        GoalKnowledgePreference preference = goalKnowledgePreferenceService.read(goal);
        QueryProfile queryProfile = queryProfile(goal);
        Map<Long, KnowledgeDocument> documentById = documents.stream()
                .collect(Collectors.toMap(KnowledgeDocument::getId, item -> item));
        List<KnowledgeChunk> chunks = knowledgeChunkRepository.findByDocumentIdIn(documentById.keySet());

        Map<Long, List<ScoredChunk>> scoredByDocument = new java.util.LinkedHashMap<>();
        List<ScoredChunk> scoredChunks = chunks.stream()
                .map(chunk -> new ScoredChunk(
                        chunk,
                        scoreChunk(chunk, documentById.get(chunk.getDocument().getId()), queryProfile, preference)
                ))
                .filter(item -> item.score() >= 3)
                .sorted(Comparator
                        .comparingInt(ScoredChunk::score).reversed()
                        .thenComparing(item -> item.chunk().getChunkIndex()))
                .toList();
        for (ScoredChunk chunk : scoredChunks) {
            scoredByDocument.computeIfAbsent(chunk.chunk().getDocument().getId(), ignored -> new ArrayList<>()).add(chunk);
        }

        List<ScoredChunk> selectedChunks = pickDiverseChunks(scoredChunks);
        Set<Long> selectedDocumentIds = selectedChunks.stream()
                .map(item -> item.chunk().getDocument().getId())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<KnowledgeRetrievalPreviewMatchResponse> matches = documents.stream()
                .map(document -> previewMatch(goal, document, queryProfile, preference, scoredByDocument, selectedDocumentIds))
                .filter(Objects::nonNull)
                .sorted(Comparator
                        .comparing(KnowledgeRetrievalPreviewMatchResponse::selectedForContext).reversed()
                        .thenComparing(KnowledgeRetrievalPreviewMatchResponse::score, Comparator.reverseOrder()))
                .limit(6)
                .toList();

        return new KnowledgeRetrievalPreviewResponse(
                goal.getId(),
                goal.getTitle(),
                documents.size(),
                matches.size(),
                selectedDocumentIds.size(),
                previewRules(goal, preference),
                matches
        );
    }

    public KnowledgeStrategyComparisonResponse compareGoals(Goal baseGoal, Goal compareGoal) {
        KnowledgeRetrievalPreviewResponse basePreview = previewForGoal(baseGoal);
        KnowledgeRetrievalPreviewResponse comparePreview = previewForGoal(compareGoal);
        GoalKnowledgePreferenceResponse basePreference = goalKnowledgePreferenceService.response(baseGoal);
        GoalKnowledgePreferenceResponse comparePreference = goalKnowledgePreferenceService.response(compareGoal);

        Map<Long, KnowledgeRetrievalPreviewMatchResponse> baseMatches = basePreview.matches().stream()
                .collect(Collectors.toMap(KnowledgeRetrievalPreviewMatchResponse::documentId, item -> item));
        Map<Long, KnowledgeRetrievalPreviewMatchResponse> compareMatches = comparePreview.matches().stream()
                .collect(Collectors.toMap(KnowledgeRetrievalPreviewMatchResponse::documentId, item -> item));

        List<KnowledgeStrategyComparisonDocumentResponse> onlyInBase = baseMatches.values().stream()
                .filter(match -> !compareMatches.containsKey(match.documentId()))
                .map(match -> toComparisonDocument(match, null))
                .toList();
        List<KnowledgeStrategyComparisonDocumentResponse> onlyInCompare = compareMatches.values().stream()
                .filter(match -> !baseMatches.containsKey(match.documentId()))
                .map(match -> toComparisonDocument(null, match))
                .toList();
        List<KnowledgeStrategyComparisonDocumentResponse> sharedDocuments = baseMatches.values().stream()
                .filter(match -> compareMatches.containsKey(match.documentId()))
                .map(match -> toComparisonDocument(match, compareMatches.get(match.documentId())))
                .sorted(Comparator.comparingInt(
                        (KnowledgeStrategyComparisonDocumentResponse item) ->
                                Math.abs(item.scoreDelta() == null ? 0 : item.scoreDelta())
                ).reversed())
                .toList();

        return new KnowledgeStrategyComparisonResponse(
                baseGoal.getId(),
                baseGoal.getTitle(),
                compareGoal.getId(),
                compareGoal.getTitle(),
                basePreference,
                comparePreference,
                compareDifferences(baseGoal, compareGoal, basePreference, comparePreference),
                onlyInBase,
                onlyInCompare,
                sharedDocuments
        );
    }

    public KnowledgeBasisResponse basisForGoal(Goal goal, List<String> evidence) {
        GoalKnowledgePreferenceResponse preference = goalKnowledgePreferenceService.response(goal);
        KnowledgeRetrievalPreviewResponse preview = previewForGoal(goal);
        List<String> knowledgeEvidence = cleanKnowledgeEvidence(evidence);
        List<KnowledgeBasisDocumentResponse> documents = preview.matches().stream()
                .filter(KnowledgeRetrievalPreviewMatchResponse::selectedForContext)
                .limit(4)
                .map(match -> new KnowledgeBasisDocumentResponse(
                        match.documentId(),
                        match.title(),
                        match.scope(),
                        match.sourceCategory(),
                        match.selectedForContext(),
                        match.reasons()
                ))
                .toList();
        List<String> titles = documents.stream().map(KnowledgeBasisDocumentResponse::title).toList();
        return new KnowledgeBasisResponse(
                knowledgeBasisSummary(goal, titles, knowledgeEvidence),
                preference,
                titles,
                knowledgeEvidence,
                documents
        );
    }

    private KnowledgeContextBundle fallbackFromSummaries(
            Goal goal,
            List<KnowledgeDocument> documents,
            QueryProfile queryProfile
    ) {
        GoalKnowledgePreference preference = goalKnowledgePreferenceService.read(goal);
        List<KnowledgeDocument> ranked = documents.stream()
                .sorted(Comparator.comparingInt(
                        document -> -scoreDocument(document, queryProfile, preference)
                ))
                .limit(MAX_DOCUMENTS)
                .toList();

        List<String> evidence = ranked.stream()
                .map(document -> formatEvidence(
                        goal,
                        document,
                        firstPresent(document.getSummary(), document.getOriginalFileName())
                ))
                .toList();
        List<String> contextSections = ranked.stream()
                .map(document -> formatContextSection(
                        document,
                        firstPresent(document.getSummary(), document.getOriginalFileName()),
                        scoreDocument(document, queryProfile, preference)
                ))
                .toList();
        List<String> titles = ranked.stream().map(KnowledgeDocument::getTitle).toList();
        return new KnowledgeContextBundle(String.join("\n\n", contextSections), evidence, titles, titles.size());
    }

    private List<KnowledgeDocument> readyDocuments(Goal goal) {
        List<KnowledgeDocument> documents = knowledgeDocumentRepository.findByUserIdAndEnabledTrueAndStatusOrderByUpdatedAtDesc(
                goal.getUser().getId(),
                KnowledgeDocumentStatus.READY
        );
        GoalKnowledgePreference preference = goalKnowledgePreferenceService.read(goal);
        List<KnowledgeDocument> filtered = documents.stream()
                .filter(document -> matchesPreference(document, preference))
                .toList();
        return filtered.isEmpty() ? documents : filtered;
    }

    private int scoreChunk(
            KnowledgeChunk chunk,
            KnowledgeDocument document,
            QueryProfile queryProfile,
            GoalKnowledgePreference preference
    ) {
        if (document == null) {
            return 0;
        }
        int score = scoreDocument(document, queryProfile, preference);
        score += scoreText(chunk.getContent(), queryProfile.primaryTokens()) * 6;
        score += scoreText(chunk.getContent(), queryProfile.secondaryTokens()) * 3;
        score += countPhraseMatches(chunk.getContent(), queryProfile.phrases()) * 8;
        score += shorterChunkBonus(chunk);
        return score;
    }

    private int scoreDocument(
            KnowledgeDocument document,
            QueryProfile queryProfile,
            GoalKnowledgePreference preference
    ) {
        int score = 0;
        score += scopeWeight(document);
        score += priorityWeight(document);
        score += preferredDocumentWeight(document, preference);
        score += recencyWeight(document);
        score += scoreText(document.getTitle(), queryProfile.primaryTokens()) * 5;
        score += scoreText(document.getTitle(), queryProfile.secondaryTokens()) * 2;
        score += scoreText(document.getSummary(), queryProfile.primaryTokens()) * 4;
        score += scoreText(document.getSummary(), queryProfile.secondaryTokens()) * 2;
        score += countPhraseMatches(document.getSummary(), queryProfile.phrases()) * 6;
        return score;
    }

    private KnowledgeRetrievalPreviewMatchResponse previewMatch(
            Goal goal,
            KnowledgeDocument document,
            QueryProfile queryProfile,
            GoalKnowledgePreference preference,
            Map<Long, List<ScoredChunk>> scoredByDocument,
            Set<Long> selectedDocumentIds
    ) {
        List<ScoredChunk> documentChunks = scoredByDocument.getOrDefault(document.getId(), List.of());
        int documentScore = documentChunks.isEmpty()
                ? scoreDocument(document, queryProfile, preference)
                : documentChunks.get(0).score();
        if (documentScore <= 0) {
            return null;
        }

        List<String> reasons = previewReasons(goal, document, queryProfile, preference, documentChunks);
        List<String> excerpts = documentChunks.isEmpty()
                ? List.of(shorten(firstPresent(document.getSummary(), document.getOriginalFileName()), 140))
                : documentChunks.stream()
                        .limit(2)
                        .map(item -> shorten(item.chunk().getContent(), 140))
                        .toList();

        return new KnowledgeRetrievalPreviewMatchResponse(
                document.getId(),
                document.getTitle(),
                document.getScope().name(),
                document.getSourceCategory().name(),
                document.getRetrievalPriority(),
                document.getGroupName(),
                document.getTags(),
                documentScore,
                selectedDocumentIds.contains(document.getId()),
                reasons,
                excerpts
        );
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

    private QueryProfile queryProfile(Goal goal) {
        Set<String> primaryTokens = new LinkedHashSet<>();
        primaryTokens.addAll(tokenize(goal.getTitle()));
        primaryTokens.addAll(tokenize(goal.getDescription()));

        Set<String> secondaryTokens = new LinkedHashSet<>();
        if (goal.getUser().getBackground() != null) {
            secondaryTokens.addAll(tokenize(goal.getUser().getBackground()));
        }

        LinkedHashSet<String> phrases = new LinkedHashSet<>();
        addPhrase(phrases, goal.getTitle());
        addPhrase(phrases, goal.getDescription());
        addPhrase(phrases, goal.getUser().getBackground());

        return new QueryProfile(primaryTokens, secondaryTokens, phrases);
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

    private List<ScoredChunk> pickDiverseChunks(List<ScoredChunk> scoredChunks) {
        List<ScoredChunk> selected = new ArrayList<>();
        Set<Long> pickedDocuments = new LinkedHashSet<>();
        for (ScoredChunk scoredChunk : scoredChunks) {
            Long documentId = scoredChunk.chunk().getDocument().getId();
            long pickedCount = selected.stream()
                    .filter(item -> Objects.equals(item.chunk().getDocument().getId(), documentId))
                    .count();
            if (pickedCount >= MAX_CHUNKS_PER_DOCUMENT) {
                continue;
            }
            if (pickedDocuments.size() >= MAX_DOCUMENTS && !pickedDocuments.contains(documentId)) {
                continue;
            }
            selected.add(scoredChunk);
            pickedDocuments.add(documentId);
            if (selected.size() >= MAX_CONTEXT_SECTIONS) {
                break;
            }
        }
        if (selected.isEmpty() && !scoredChunks.isEmpty()) {
            return scoredChunks.stream().limit(Math.min(MAX_CONTEXT_SECTIONS, scoredChunks.size())).toList();
        }
        return selected;
    }

    private int countPhraseMatches(String text, Collection<String> phrases) {
        if (text == null || text.isBlank() || phrases.isEmpty()) {
            return 0;
        }
        String normalized = text.toLowerCase(Locale.ROOT);
        int score = 0;
        for (String phrase : phrases) {
            if (normalized.contains(phrase)) {
                score++;
            }
        }
        return score;
    }

    private int scopeWeight(KnowledgeDocument document) {
        return document.getScope() == KnowledgeDocumentScope.PERSONAL ? 8 : 3;
    }

    private int priorityWeight(KnowledgeDocument document) {
        int priority = document.getRetrievalPriority() == null ? 3 : document.getRetrievalPriority();
        return priority * 2;
    }

    private int recencyWeight(KnowledgeDocument document) {
        if (document.getImportedAt() != null) {
            return 2;
        }
        if (document.getUpdatedAt() != null) {
            return 1;
        }
        return 0;
    }

    private int shorterChunkBonus(KnowledgeChunk chunk) {
        if (chunk.getCharacterCount() == null) {
            return 0;
        }
        if (chunk.getCharacterCount() <= 500) {
            return 2;
        }
        if (chunk.getCharacterCount() <= 800) {
            return 1;
        }
        return 0;
    }

    private void addPhrase(Set<String> values, String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        String normalized = text.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
        if (normalized.length() >= 4) {
            values.add(normalized);
        }
    }

    private List<String> previewReasons(
            Goal goal,
            KnowledgeDocument document,
            QueryProfile queryProfile,
            GoalKnowledgePreference preference,
            List<ScoredChunk> documentChunks
    ) {
        List<String> values = new ArrayList<>();
        if (preference.preferredDocumentIds().contains(document.getId())) {
            values.add(isZh(goal) ? "该文档被当前目标固定为优先资料" : "This document is pinned as a preferred material for the goal");
        }
        if (document.getScope() == KnowledgeDocumentScope.PERSONAL) {
            values.add(isZh(goal) ? "个人资料作用域获得更高权重" : "Personal scope receives a higher weight");
        } else {
            values.add(isZh(goal) ? "平台资料会参与检索，但权重低于个人资料" : "Platform material participates with a lower weight than personal material");
        }
        values.add((isZh(goal) ? "检索优先级：" : "Retrieval priority: ") + document.getRetrievalPriority());
        if (scoreText(document.getTitle(), queryProfile.primaryTokens()) > 0) {
            values.add(isZh(goal) ? "标题命中目标关键词" : "Title matches goal keywords");
        }
        if (countPhraseMatches(document.getSummary(), queryProfile.phrases()) > 0) {
            values.add(isZh(goal) ? "摘要命中目标短语" : "Summary matches goal phrases");
        }
        if (!documentChunks.isEmpty()) {
            values.add(isZh(goal) ? "片段内容直接命中目标或背景词" : "Chunk content directly matches goal or background terms");
        } else if (document.getSummary() != null && !document.getSummary().isBlank()) {
            values.add(isZh(goal) ? "当前使用摘要参与排序" : "The summary is currently used for ranking");
        }
        if (document.getGroupName() != null && !document.getGroupName().isBlank()) {
            values.add((isZh(goal) ? "分组：" : "Group: ") + document.getGroupName());
        }
        if (!document.getTags().isEmpty()) {
            values.add((isZh(goal) ? "标签：" : "Tags: ") + String.join(", ", document.getTags()));
        }
        return values.stream().distinct().limit(5).toList();
    }

    private List<String> previewRules(Goal goal, GoalKnowledgePreference preference) {
        List<String> values = new ArrayList<>();
        if (isZh(goal)) {
            values.add("系统会优先考虑个人资料，其次才是平台资料。");
            values.add("检索优先级、标题/摘要命中、片段命中和最近导入时间都会影响排序。");
            values.add("同一文档最多选入少量片段，并限制最终上下文的文档多样性。");
            if (preference.preferredScope() != null) {
                values.add("当前目标已固定作用域：" + preference.preferredScope());
            }
            if (!preference.preferredCategories().isEmpty()) {
                values.add("当前目标已固定来源分类：" + String.join(", ", preference.preferredCategories()));
            }
            if (!preference.preferredDocumentIds().isEmpty()) {
                values.add("当前目标已设置优先文档，它们会获得额外排序加权。");
            }
            return values;
        }
        values.add("Personal materials are prioritized ahead of platform materials.");
        values.add("Retrieval priority, title/summary matches, chunk matches, and recency all affect ranking.");
        values.add("Only a few chunks can be selected from the same document, and the final context preserves document diversity.");
        if (preference.preferredScope() != null) {
            values.add("The goal currently fixes the scope to " + preference.preferredScope() + ".");
        }
        if (!preference.preferredCategories().isEmpty()) {
            values.add("The goal currently fixes the source categories to " + String.join(", ", preference.preferredCategories()) + ".");
        }
        if (!preference.preferredDocumentIds().isEmpty()) {
            values.add("Preferred documents are pinned and receive extra ranking weight.");
        }
        return values;
    }

    private boolean matchesPreference(KnowledgeDocument document, GoalKnowledgePreference preference) {
        boolean scopeMatches = preference.preferredScope() == null
                || preference.preferredScope().equalsIgnoreCase(document.getScope().name());
        boolean categoryMatches = preference.preferredCategories().isEmpty()
                || preference.preferredCategories().stream()
                .anyMatch(value -> value.equalsIgnoreCase(document.getSourceCategory().name()));
        return scopeMatches && categoryMatches;
    }

    private int preferredDocumentWeight(KnowledgeDocument document, GoalKnowledgePreference preference) {
        return preference.preferredDocumentIds().contains(document.getId()) ? 12 : 0;
    }

    private KnowledgeStrategyComparisonDocumentResponse toComparisonDocument(
            KnowledgeRetrievalPreviewMatchResponse baseMatch,
            KnowledgeRetrievalPreviewMatchResponse compareMatch
    ) {
        KnowledgeRetrievalPreviewMatchResponse reference = baseMatch != null ? baseMatch : compareMatch;
        Integer baseScore = baseMatch == null ? null : baseMatch.score();
        Integer compareScore = compareMatch == null ? null : compareMatch.score();
        Integer delta = baseScore == null || compareScore == null ? null : baseScore - compareScore;
        return new KnowledgeStrategyComparisonDocumentResponse(
                reference.documentId(),
                reference.title(),
                reference.scope(),
                reference.sourceCategory(),
                baseScore,
                compareScore,
                delta,
                baseMatch != null && baseMatch.selectedForContext(),
                compareMatch != null && compareMatch.selectedForContext(),
                reference.tags()
        );
    }

    private List<String> compareDifferences(
            Goal baseGoal,
            Goal compareGoal,
            GoalKnowledgePreferenceResponse basePreference,
            GoalKnowledgePreferenceResponse comparePreference
    ) {
        List<String> values = new ArrayList<>();
        if (!Objects.equals(basePreference.preferredScope(), comparePreference.preferredScope())) {
            values.add(isZh(baseGoal)
                    ? "两个目标固定的作用域不同。"
                    : "The two goals use different fixed scopes.");
        }
        if (!basePreference.preferredCategories().equals(comparePreference.preferredCategories())) {
            values.add(isZh(baseGoal)
                    ? "两个目标固定的来源分类不同。"
                    : "The two goals use different fixed source categories.");
        }
        if (!basePreference.preferredDocumentIds().equals(comparePreference.preferredDocumentIds())) {
            values.add(isZh(baseGoal)
                    ? "两个目标固定的优先文档不同。"
                    : "The two goals pin different preferred documents.");
        }
        if (values.isEmpty()) {
            values.add(isZh(baseGoal)
                    ? "两个目标当前使用相同的知识偏好，差异主要来自目标文本本身。"
                    : "Both goals currently use the same knowledge preference, so the main differences come from the goal text itself.");
        }
        values.add(isZh(baseGoal)
                ? "对比结果基于各自目标下的当前检索预览命中文档。"
                : "The comparison is based on the current retrieval preview for each goal.");
        return values;
    }

    private String formatContextSection(KnowledgeDocument document, String content, int score) {
        return "Source: " + document.getTitle() + "\n"
                + "Scope: " + document.getScope().name() + "\n"
                + "Summary: " + firstPresent(document.getSummary(), "No summary available.") + "\n"
                + "Relevance: " + score + "\n"
                + "Excerpt: " + shorten(content, 260);
    }

    private String formatEvidence(Goal goal, KnowledgeDocument document, String content) {
        String scopeLabel = document.getScope() == KnowledgeDocumentScope.PERSONAL
                ? (isZh(goal) ? "个人资料" : "personal")
                : (isZh(goal) ? "平台资料" : "platform");
        if (isZh(goal)) {
            return "知识库证据《" + document.getTitle() + "》[" + scopeLabel + "]：" + shorten(content, 120);
        }
        return "Knowledge evidence from \"" + document.getTitle() + "\" [" + scopeLabel + "]: " + shorten(content, 120);
    }

    private List<String> cleanKnowledgeEvidence(List<String> evidence) {
        if (evidence == null) {
            return List.of();
        }
        return evidence.stream()
                .filter(value -> value != null && !value.isBlank())
                .filter(value -> value.contains("知识库证据") || value.contains("Knowledge evidence"))
                .map(String::trim)
                .distinct()
                .limit(4)
                .toList();
    }

    private String knowledgeBasisSummary(Goal goal, List<String> titles, List<String> evidence) {
        if (isZh(goal)) {
            if (!titles.isEmpty()) {
                return "本次结果主要参考了 " + String.join("、", titles) + " 等知识资料。";
            }
            if (!evidence.isEmpty()) {
                return "本次结果引用了已启用知识库中的证据片段。";
            }
            return "当前没有明确记录到知识库引用，后续可在知识库中继续补充资料。";
        }
        if (!titles.isEmpty()) {
            return "This result mainly references knowledge materials such as " + String.join(", ", titles) + ".";
        }
        if (!evidence.isEmpty()) {
            return "This result includes evidence snippets from the enabled knowledge base.";
        }
        return "No explicit knowledge-base reference is currently recorded for this result.";
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

    private record QueryProfile(
            Set<String> primaryTokens,
            Set<String> secondaryTokens,
            Set<String> phrases
    ) {
    }
}

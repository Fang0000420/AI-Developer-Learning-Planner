package com.aidevplanner.backend.knowledge;

import com.aidevplanner.backend.goal.Goal;
import com.aidevplanner.backend.goal.GoalKnowledgePreference;
import com.aidevplanner.backend.goal.GoalKnowledgePreferenceService;
import com.aidevplanner.backend.goal.ResponseLanguage;
import com.aidevplanner.backend.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KnowledgeContextServiceTests {

    @Mock
    private KnowledgeChunkRepository knowledgeChunkRepository;

    @Mock
    private KnowledgeDocumentRepository knowledgeDocumentRepository;

    @Mock
    private GoalKnowledgePreferenceService goalKnowledgePreferenceService;

    private KnowledgeContextService knowledgeContextService;

    @BeforeEach
    void setUp() {
        knowledgeContextService = new KnowledgeContextService(
                knowledgeChunkRepository,
                knowledgeDocumentRepository,
                goalKnowledgePreferenceService
        );
        when(goalKnowledgePreferenceService.read(org.mockito.ArgumentMatchers.any(Goal.class)))
                .thenReturn(new GoalKnowledgePreference(List.of(), null, List.of()));
    }

    @Test
    void prioritizesPersonalKnowledgeAndKeepsDocumentDiversity() {
        Goal goal = goal();
        KnowledgeDocument personal = readyDocument(
                goal,
                11L,
                KnowledgeDocumentScope.PERSONAL,
                "Resume",
                "Java backend work on AI agents and Spring services."
        );
        KnowledgeDocument platform = readyDocument(
                goal,
                12L,
                KnowledgeDocumentScope.PLATFORM,
                "Platform guide",
                "Generic AI agent workflow guide with evaluation checklist."
        );
        KnowledgeChunk personalChunk1 = chunk(personal, 1, "Built AI agent apps with Java and Spring Boot in production.");
        KnowledgeChunk personalChunk2 = chunk(personal, 2, "Improved evaluation loops and structured output validation.");
        KnowledgeChunk platformChunk = chunk(platform, 1, "AI agent workflow guide for design and evaluation basics.");

        when(knowledgeDocumentRepository.findByUserIdAndEnabledTrueAndStatusOrderByUpdatedAtDesc(1L, KnowledgeDocumentStatus.READY))
                .thenReturn(List.of(personal, platform));
        when(knowledgeChunkRepository.findByDocumentIdIn(anyList()))
                .thenReturn(List.of(personalChunk1, personalChunk2, platformChunk));

        KnowledgeContextBundle bundle = knowledgeContextService.buildForGoal(goal);

        assertThat(bundle.documentTitles()).containsExactly("Resume", "Platform guide");
        assertThat(bundle.evidence().get(0)).contains("个人资料");
        assertThat(bundle.contextText()).contains("Scope: PERSONAL");
        assertThat(bundle.contextText()).contains("Scope: PLATFORM");
        assertThat(bundle.documentCount()).isEqualTo(2);
    }

    @Test
    void fallsBackToSummariesWhenChunksAreUnavailable() {
        Goal goal = goal();
        KnowledgeDocument personal = readyDocument(
                goal,
                21L,
                KnowledgeDocumentScope.PERSONAL,
                "Interview notes",
                "Needs short business English speaking drills for work scenarios."
        );
        KnowledgeDocument platform = readyDocument(
                goal,
                22L,
                KnowledgeDocumentScope.PLATFORM,
                "Speaking guide",
                "General communication practice routine."
        );

        when(knowledgeDocumentRepository.findByUserIdAndEnabledTrueAndStatusOrderByUpdatedAtDesc(1L, KnowledgeDocumentStatus.READY))
                .thenReturn(List.of(personal, platform));
        when(knowledgeChunkRepository.findByDocumentIdIn(anyList()))
                .thenReturn(List.of());

        KnowledgeContextBundle bundle = knowledgeContextService.buildForGoal(goal);

        assertThat(bundle.contextText()).contains("Interview notes");
        assertThat(bundle.evidence().get(0)).contains("个人资料");
        assertThat(bundle.documentTitles()).containsExactly("Interview notes", "Speaking guide");
    }

    private Goal goal() {
        User user = new User("learner", "learner@example.com", "hash");
        user.setBackground("Java backend engineer moving into AI agent app development.");
        ReflectionTestUtils.setField(user, "id", 1L);

        Goal goal = new Goal(user, "Build AI agent apps", 30);
        goal.setDescription("Use Spring and evaluation loops to build production-ready AI agent workflow.");
        goal.setResponseLanguage(ResponseLanguage.zh);
        ReflectionTestUtils.setField(goal, "id", 10L);
        return goal;
    }

    private KnowledgeDocument readyDocument(
            Goal goal,
            Long id,
            KnowledgeDocumentScope scope,
            String title,
            String summary
    ) {
        KnowledgeDocument document = new KnowledgeDocument(
                goal.getUser(),
                scope,
                title,
                scope == KnowledgeDocumentScope.PERSONAL ? "personal_upload" : "platform_seed",
                title + ".md",
                "text/markdown",
                1024L,
                "storage"
        );
        ReflectionTestUtils.setField(document, "id", id);
        ReflectionTestUtils.setField(document, "status", KnowledgeDocumentStatus.READY);
        ReflectionTestUtils.setField(document, "summary", summary);
        ReflectionTestUtils.setField(document, "importedAt", LocalDateTime.of(2026, 6, 13, 9, 0));
        ReflectionTestUtils.setField(document, "updatedAt", LocalDateTime.of(2026, 6, 13, 9, 0));
        ReflectionTestUtils.setField(document, "enabled", true);
        return document;
    }

    private KnowledgeChunk chunk(KnowledgeDocument document, int index, String content) {
        KnowledgeChunk chunk = new KnowledgeChunk(document, index, content, content.length());
        ReflectionTestUtils.setField(chunk, "id", (long) index);
        return chunk;
    }
}

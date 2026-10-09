package domain.listening

import core.common.getOrThrow
import domain.focus.model.LearningFocus
import domain.focus.usecase.ObserveLearningFocusUseCase
import domain.listening.model.LanguageVoice
import domain.listening.model.ListeningOrder
import domain.listening.model.ListeningSettings
import domain.listening.model.VoiceStatus
import domain.listening.usecase.BuildListeningQueueUseCase
import domain.listening.usecase.CheckListeningVoicesUseCase
import domain.listening.usecase.SaveListeningSettingsUseCase
import domain.settings.usecase.GetDailyGoalWordsUseCase
import domain.word.model.ReviewSource
import domain.word.model.Word
import domain.word.usecase.GetDueWordsByTagUseCase
import domain.word.usecase.GetDueWordsUseCase
import domain.word.usecase.GetWordsByStageUseCase
import domain.word.usecase.LoadReviewQueueUseCase
import fakes.FakeLearningFocusRepository
import fakes.FakeListeningSettingsRepository
import fakes.FakeSettingsRepository
import fakes.FakeTtsRepository
import fakes.FakeWordRepository
import kotlinx.coroutines.test.runTest
import utils.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ListeningUseCasesTest {

    private fun word(
        id: Int,
        target: Language = Language.GERMAN,
        source: Language = Language.ENGLISH,
        dateAdded: Long = id.toLong(),
    ) = Word(
        id = id,
        originalWord = "w$id",
        translation = "t$id",
        description = "",
        sourceLanguage = source,
        targetLanguage = target,
        nextReviewDate = 0L,
        dateAdded = dateAdded,
    )

    private fun buildQueue(repo: FakeWordRepository, focus: LearningFocus? = LearningFocus.All): BuildListeningQueueUseCase {
        val focusRepo = FakeLearningFocusRepository(focus)
        val observeFocus = ObserveLearningFocusUseCase(repo, focusRepo)
        val loadQueue = LoadReviewQueueUseCase(
            GetDueWordsUseCase(repo),
            GetWordsByStageUseCase(repo),
            GetDueWordsByTagUseCase(repo),
            GetDailyGoalWordsUseCase(FakeSettingsRepository()),
            observeFocus,
        )
        return BuildListeningQueueUseCase(loadQueue, repo, observeFocus)
    }

    // --- BuildListeningQueueUseCase ---

    @Test
    fun `build queue returns due words when any are due`() = runTest {
        val repo = FakeWordRepository().apply {
            dueWords = listOf(word(1), word(2))
            storedWords = mutableListOf(word(1), word(2), word(3))
        }

        val queue = buildQueue(repo)(ReviewSource.DueCards).getOrThrow()

        assertEquals(listOf(1, 2), queue.words.map { it.id })
        assertFalse(queue.isRecentFallback)
    }

    @Test
    fun `build queue falls back to newest words in focus when nothing is due`() = runTest {
        val repo = FakeWordRepository().apply {
            storedWords = mutableListOf(
                word(1, dateAdded = 10),
                word(2, dateAdded = 30),
                word(3, target = Language.SPANISH, dateAdded = 40),
                word(4, dateAdded = 20),
            )
        }

        val queue = buildQueue(repo, LearningFocus.Single(Language.GERMAN))(ReviewSource.DueCards).getOrThrow()

        assertEquals(listOf(2, 4, 1), queue.words.map { it.id })
        assertTrue(queue.isRecentFallback)
    }

    @Test
    fun `build queue fallback is capped`() = runTest {
        val repo = FakeWordRepository().apply {
            storedWords = (1..50).map { word(it) }.toMutableList()
        }

        val queue = buildQueue(repo)(ReviewSource.DueCards).getOrThrow()

        assertEquals(BuildListeningQueueUseCase.RECENT_FALLBACK_LIMIT, queue.words.size)
    }

    @Test
    fun `build queue is empty without fallback flag when there are no words`() = runTest {
        val queue = buildQueue(FakeWordRepository())(ReviewSource.DueCards).getOrThrow()

        assertTrue(queue.words.isEmpty())
        assertFalse(queue.isRecentFallback)
    }

    // --- CheckListeningVoicesUseCase ---

    @Test
    fun `check voices reports each distinct language once with its status`() = runTest {
        val tts = FakeTtsRepository().apply {
            missingLanguages += "de"
            unsupportedLanguages = setOf("es")
        }
        val words = listOf(word(1), word(2), word(3, target = Language.SPANISH))

        val check = CheckListeningVoicesUseCase(tts)(words).getOrThrow()

        assertEquals(
            listOf(
                LanguageVoice("de", VoiceStatus.MISSING),
                LanguageVoice("en", VoiceStatus.READY),
                LanguageVoice("es", VoiceStatus.UNSUPPORTED),
            ),
            check.voices,
        )
        assertTrue(check.canSpeakAnything)
    }

    @Test
    fun `check voices cannot speak anything when every language is unsupported`() = runTest {
        val tts = FakeTtsRepository().apply { languageSupported = false }

        val check = CheckListeningVoicesUseCase(tts)(listOf(word(1))).getOrThrow()

        assertFalse(check.canSpeakAnything)
    }

    // --- SaveListeningSettingsUseCase ---

    @Test
    fun `save settings clamps pause and repeat to supported ranges`() = runTest {
        val repo = FakeListeningSettingsRepository()

        SaveListeningSettingsUseCase(repo)(
            ListeningSettings(pauseMs = 60_000L, order = ListeningOrder.TRANSLATION_FIRST, repeatCount = 9)
        ).getOrThrow()

        assertEquals(
            ListeningSettings(pauseMs = 5_000L, order = ListeningOrder.TRANSLATION_FIRST, repeatCount = 2),
            repo.settings.value,
        )
    }
}

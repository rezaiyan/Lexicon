package domain.listening

import core.common.getOrThrow
import domain.focus.model.LearningFocus
import domain.focus.usecase.ObserveLearningFocusUseCase
import domain.listening.model.LanguageVoice
import domain.listening.model.ListeningLevelOption
import domain.listening.model.ListeningOptions
import domain.listening.model.ListeningOrder
import domain.listening.model.ListeningSelection
import domain.listening.model.ListeningSettings
import domain.listening.model.ListeningSource
import domain.listening.model.ListeningTagOption
import domain.listening.model.VoiceStatus
import domain.listening.usecase.BuildListeningQueueUseCase
import domain.listening.usecase.CheckListeningVoicesUseCase
import domain.listening.usecase.ObserveListeningOptionsUseCase
import domain.listening.usecase.SaveListeningSettingsUseCase
import domain.tag.model.Tag
import domain.word.model.LearningStage
import domain.word.model.Word
import fakes.FakeLearningFocusRepository
import fakes.FakeListeningSettingsRepository
import fakes.FakeTagRepository
import fakes.FakeTtsRepository
import fakes.FakeWordRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import utils.Language
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ListeningUseCasesTest {

    private fun word(
        id: Int,
        target: Language = Language.GERMAN,
        source: Language = Language.ENGLISH,
        dateAdded: Long = id.toLong(),
        level: Int = 0,
        tagIds: List<Long> = emptyList(),
    ) = Word(
        id = id,
        originalWord = "w$id",
        translation = "t$id",
        description = "",
        sourceLanguage = source,
        targetLanguage = target,
        level = level,
        nextReviewDate = 0L,
        dateAdded = dateAdded,
        tagIds = tagIds,
    )

    private fun observeFocus(repo: FakeWordRepository, focus: LearningFocus? = LearningFocus.All) =
        ObserveLearningFocusUseCase(repo, FakeLearningFocusRepository(focus))

    private fun buildQueue(repo: FakeWordRepository, focus: LearningFocus? = LearningFocus.All) =
        BuildListeningQueueUseCase(repo, observeFocus(repo, focus), Random(SEED))

    private fun observeOptions(
        repo: FakeWordRepository,
        tags: List<Tag> = emptyList(),
        focus: LearningFocus? = LearningFocus.All,
    ) = ObserveListeningOptionsUseCase(repo, FakeTagRepository().apply { this.tags = tags }, observeFocus(repo, focus))

    private fun tag(id: Long, name: String) = Tag(id, name, createdAt = 0L, updatedAt = 0L)

    // --- ObserveListeningOptionsUseCase ---

    @Test
    fun `options have no words when the library is empty`() = runTest {
        val options = observeOptions(FakeWordRepository())(Unit).first()

        assertFalse(options.hasWords)
        assertEquals(ListeningOptions(), options)
    }

    @Test
    fun `options count due, all, each level and each tag within the focus`() = runTest {
        val repo = FakeWordRepository().apply {
            storedWords = mutableListOf(
                word(1, level = 0, tagIds = listOf(10L)),
                word(2, level = 0, tagIds = listOf(10L, 20L)),
                word(3, level = 3),
                word(4, level = 3, target = Language.SPANISH, tagIds = listOf(30L)),
            )
            dueWords = listOf(word(1), word(4, target = Language.SPANISH))
        }
        val tags = listOf(tag(20L, "verbs"), tag(10L, "Animals"), tag(30L, "spanish only"), tag(40L, "empty"))

        val options = observeOptions(repo, tags, LearningFocus.Single(Language.GERMAN))(Unit).first()

        assertEquals(1, options.dueCount)
        assertEquals(3, options.allCount)
        assertEquals(
            listOf(
                ListeningLevelOption(LearningStage.LEVEL_0_FRESH, 2),
                ListeningLevelOption(LearningStage.LEVEL_3_BUILDING, 1),
            ),
            options.levels,
        )
        // Sorted by name, case-insensitively; tags with no focused words are left out.
        assertEquals(
            listOf(ListeningTagOption(10L, "Animals", 2), ListeningTagOption(20L, "verbs", 1)),
            options.tags,
        )
    }

    @Test
    fun `resolve keeps a source with words and falls back to every word otherwise`() {
        val options = ListeningOptions(dueCount = 0, allCount = 5, tags = listOf(ListeningTagOption(1L, "a", 2)))
        val tagged = ListeningSelection(ListeningSource.Tag(1L), limit = 10, shuffle = true)

        assertEquals(tagged, options.resolve(tagged))
        assertEquals(
            ListeningSelection(ListeningSource.All, limit = 10),
            options.resolve(ListeningSelection(ListeningSource.Due, limit = 10)),
        )
        assertEquals(ListeningSource.All, options.resolve(ListeningSelection(ListeningSource.Tag(99L))).source)
    }

    // --- BuildListeningQueueUseCase ---

    @Test
    fun `due source plays due words in the focus, capped at the limit`() = runTest {
        val repo = FakeWordRepository().apply {
            dueWords = (1..30).map { word(it) } + word(99, target = Language.SPANISH)
        }

        val words = buildQueue(repo, LearningFocus.Single(Language.GERMAN))(ListeningSelection()).getOrThrow()

        assertEquals((1..ListeningSelection.DEFAULT_LIMIT).toList(), words.map { it.id })
    }

    @Test
    fun `all source plays every word in the focus newest first`() = runTest {
        val repo = FakeWordRepository().apply {
            storedWords = mutableListOf(
                word(1, dateAdded = 10),
                word(2, dateAdded = 30),
                word(3, target = Language.SPANISH, dateAdded = 40),
                word(4, dateAdded = 20),
            )
        }
        val selection = ListeningSelection(ListeningSource.All, limit = ListeningSelection.LIMIT_ALL)

        val words = buildQueue(repo, LearningFocus.Single(Language.GERMAN))(selection).getOrThrow()

        assertEquals(listOf(2, 4, 1), words.map { it.id })
    }

    @Test
    fun `level source plays only words at that level`() = runTest {
        val repo = FakeWordRepository().apply {
            storedWords = mutableListOf(word(1, level = 2), word(2, level = 5), word(3, level = 2))
        }

        val words = buildQueue(repo)(ListeningSelection(ListeningSource.Level(LearningStage.LEVEL_2_FAMILIAR)))
            .getOrThrow()

        assertEquals(setOf(1, 3), words.map { it.id }.toSet())
    }

    @Test
    fun `tag source plays only words with that tag`() = runTest {
        val repo = FakeWordRepository().apply {
            storedWords = mutableListOf(word(1, tagIds = listOf(7L)), word(2), word(3, tagIds = listOf(8L, 7L)))
        }

        val words = buildQueue(repo)(ListeningSelection(ListeningSource.Tag(7L))).getOrThrow()

        assertEquals(setOf(1, 3), words.map { it.id }.toSet())
    }

    @Test
    fun `limit caps the session and LIMIT_ALL plays everything`() = runTest {
        val repo = FakeWordRepository().apply { storedWords = (1..60).map { word(it) }.toMutableList() }

        val capped = buildQueue(repo)(ListeningSelection(ListeningSource.All, limit = 30)).getOrThrow()
        val all = buildQueue(repo)(ListeningSelection(ListeningSource.All, ListeningSelection.LIMIT_ALL)).getOrThrow()

        assertEquals(30, capped.size)
        assertEquals(60, all.size)
    }

    @Test
    fun `shuffle plays the same words in a different order`() = runTest {
        val repo = FakeWordRepository().apply { storedWords = (1..20).map { word(it) }.toMutableList() }
        val ordered = ListeningSelection(ListeningSource.All, ListeningSelection.LIMIT_ALL)

        val plain = buildQueue(repo)(ordered).getOrThrow().map { it.id }
        val shuffled = buildQueue(repo)(ordered.copy(shuffle = true)).getOrThrow().map { it.id }

        assertEquals(plain.toSet(), shuffled.toSet())
        assertNotEquals(plain, shuffled)
    }

    @Test
    fun `shuffle picks from the whole source before capping`() = runTest {
        val repo = FakeWordRepository().apply { storedWords = (1..100).map { word(it) }.toMutableList() }

        val words = buildQueue(repo)(ListeningSelection(ListeningSource.All, limit = 10, shuffle = true)).getOrThrow()

        // Newest-first without shuffle would be exactly 100 downTo 91.
        assertNotEquals((100 downTo 91).toList(), words.map { it.id })
        assertEquals(10, words.size)
    }

    @Test
    fun `build queue is empty when there are no words`() = runTest {
        assertTrue(buildQueue(FakeWordRepository())(ListeningSelection()).getOrThrow().isEmpty())
    }

    @Test
    fun `session size is the limit or what is available, whichever is smaller`() {
        assertEquals(10, ListeningSelection(limit = 10).sessionSize(available = 40))
        assertEquals(4, ListeningSelection(limit = 10).sessionSize(available = 4))
        assertEquals(40, ListeningSelection(limit = ListeningSelection.LIMIT_ALL).sessionSize(available = 40))
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

    @Test
    fun `save settings turns a negative word limit into play-all`() = runTest {
        val repo = FakeListeningSettingsRepository()

        SaveListeningSettingsUseCase(repo)(ListeningSettings(selection = ListeningSelection(limit = -5))).getOrThrow()

        assertEquals(ListeningSelection.LIMIT_ALL, repo.settings.value.selection.limit)
    }

    private companion object {
        const val SEED = 42
    }
}

package domain.focus

import core.common.getOrThrow
import domain.focus.model.LearningFocus
import domain.focus.usecase.AcknowledgeFocusIntroUseCase
import domain.focus.usecase.DismissFocusNudgeUseCase
import domain.focus.usecase.ObserveLearningFocusUseCase
import domain.focus.usecase.ObserveStudyFocusUseCase
import domain.focus.usecase.SetLearningFocusUseCase
import domain.tag.model.Tag
import domain.word.model.Word
import fakes.FakeLearningFocusRepository
import fakes.FakeTagRepository
import fakes.FakeWordRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import utils.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FocusUseCasesTest {

    private val now = 1_000_000_000L

    private fun word(id: Int, language: Language, tags: List<Long> = emptyList()) = Word(
        id = id,
        originalWord = "w$id",
        translation = "t$id",
        description = "",
        sourceLanguage = Language.ENGLISH,
        targetLanguage = language,
        nextReviewDate = now - 1,
        tagIds = tags,
    )

    private val words = buildList {
        add(word(1, Language.GERMAN, tags = listOf(7L)))
        repeat(10) { add(word(100 + it, Language.SPANISH)) }
    }

    private val wordRepo = FakeWordRepository().apply { storedWords = words.toMutableList() }
    private val tagRepo = FakeTagRepository().apply {
        tags = listOf(Tag(id = 7L, name = "Food", wordCount = 0L, createdAt = 0L, updatedAt = 0L))
    }

    @Test
    fun `observe learning focus resolves stored preference`() = runTest {
        val focusRepo = FakeLearningFocusRepository(LearningFocus.Single(Language.GERMAN))
        val useCase = ObserveLearningFocusUseCase(wordRepo, focusRepo) { now }
        assertEquals(LearningFocus.Single(Language.GERMAN), useCase().first())
    }

    @Test
    fun `study focus overview scopes stats tags and exposes nudge and intro`() = runTest {
        val focusRepo = FakeLearningFocusRepository(LearningFocus.Single(Language.GERMAN))
        val overview = ObserveStudyFocusUseCase(wordRepo, tagRepo, focusRepo) { now }().first()

        assertEquals(LearningFocus.Single(Language.GERMAN), overview.focus)
        assertEquals(1, overview.progressStats.totalWords)
        assertEquals(listOf(7L), overview.tagStats.tags.map { it.id })
        assertEquals(Language.SPANISH, overview.nudge?.language)
        assertTrue(overview.showIntro)
        assertEquals(2, overview.languages.size)
    }

    @Test
    fun `set focus then overview re-emits with new scope`() = runTest {
        val focusRepo = FakeLearningFocusRepository(LearningFocus.Single(Language.GERMAN))
        SetLearningFocusUseCase(focusRepo)(LearningFocus.Single(Language.SPANISH)).getOrThrow()
        val overview = ObserveStudyFocusUseCase(wordRepo, tagRepo, focusRepo) { now }().first()
        assertEquals(10, overview.progressStats.totalWords)
        assertNull(overview.nudge)
    }

    @Test
    fun `dismiss nudge stores today and hides nudge`() = runTest {
        val focusRepo = FakeLearningFocusRepository(LearningFocus.Single(Language.GERMAN))
        DismissFocusNudgeUseCase(focusRepo) { now }().getOrThrow()
        assertEquals(LearningFocusPolicy.epochDay(now), focusRepo.nudgeDismissedDay.value)
        val overview = ObserveStudyFocusUseCase(wordRepo, tagRepo, focusRepo) { now }().first()
        assertNull(overview.nudge)
    }

    @Test
    fun `acknowledge intro hides intro`() = runTest {
        val focusRepo = FakeLearningFocusRepository()
        AcknowledgeFocusIntroUseCase(focusRepo)().getOrThrow()
        val overview = ObserveStudyFocusUseCase(wordRepo, tagRepo, focusRepo) { now }().first()
        assertFalse(overview.showIntro)
    }

    @Test
    fun `single language never shows intro`() = runTest {
        wordRepo.storedWords = mutableListOf(word(1, Language.GERMAN))
        val overview = ObserveStudyFocusUseCase(wordRepo, tagRepo, FakeLearningFocusRepository()) { now }().first()
        assertEquals(LearningFocus.All, overview.focus)
        assertFalse(overview.showIntro)
    }
}

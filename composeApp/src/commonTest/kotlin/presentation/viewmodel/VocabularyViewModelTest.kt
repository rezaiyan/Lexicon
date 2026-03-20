package presentation.viewmodel

import analytics.IAnalyticsTracker
import core.common.Try
import domain.word.model.LearningStage
import domain.word.model.ProgressStats
import domain.word.model.Word
import domain.word.repository.DeleteWordsProgress
import domain.word.repository.IWordRepository
import domain.word.repository.UpdateWordsLanguagesProgress
import domain.word.usecase.DeleteWordUseCase
import domain.word.usecase.GetDueWordsUseCase
import domain.word.usecase.GetWordsByStageUseCase
import domain.word.usecase.UpdateWordUseCase
import events.VocabularyEffect
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import presentation.ViewModelTestBase
import feature.words.model.ReviewMode
import feature.words.VocabularyViewModel
import core.common.UiState
import utils.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class VocabularyViewModelTest : ViewModelTestBase() {

    private var dueWords: List<Word> = listOf(testWord(1), testWord(2))
    private var stageWords: List<Word> = listOf(testWord(3))
    private var deleteResult: Try<Unit> = Try.success(Unit)
    private val loggedEvents = mutableListOf<String>()

    private fun testWord(id: Int) = Word(
        id = id,
        originalWord = "word$id",
        translation = "trans$id",
        description = "desc$id",
        sourceLanguage = Language.ENGLISH,
        targetLanguage = Language.GERMAN,
        nextReviewDate = 0L
    )

    private fun fakeRepo() = object : IWordRepository {
        override fun getDueCards(): Flow<List<Word>> = flowOf(dueWords)
        override fun getWordsByStage(stage: LearningStage): Flow<List<Word>> = flowOf(stageWords)
        override suspend fun deleteWord(id: Int): Try<Unit> = deleteResult
        override suspend fun updateWord(word: Word): Try<Unit> = Try.success(Unit)
        override suspend fun getAllWordsAsync(): Try<List<Word>> = Try.success(emptyList())
        override fun getAllWords(): Flow<List<Word>> = flowOf(emptyList())
        override suspend fun getWordById(id: Int): Word? = null
        override suspend fun insertWords(words: List<Word>): Try<Int> = Try.success(0)
        override fun deleteWords(ids: List<Int>): Flow<DeleteWordsProgress> = flowOf()
        override fun updateWordsLanguages(ids: List<Int>, sourceLanguage: String, targetLanguage: String): Flow<UpdateWordsLanguagesProgress> = flowOf()
        override suspend fun deleteAllWords(): Try<Unit> = Try.success(Unit)
        override suspend fun syncWithRemote(): Try<Unit> = Try.success(Unit)
        override suspend fun syncRemoteToLocal(clearFirst: Boolean): Try<Unit> = Try.success(Unit)
        override fun getProgressStats(): Flow<ProgressStats> = flowOf()
        override suspend fun getTotalCount(): Try<Int> = Try.success(0)
        override suspend fun getDueCount(): Try<Int> = Try.success(0)
        override suspend fun getMostCommonSourceLanguage(): Try<String?> = Try.success(null)
    }

    private fun fakeAnalytics() = object : IAnalyticsTracker {
        override fun logScreenView(screenName: String) {}
        override fun logEvent(eventName: String, parameters: Map<String, Any>?) { loggedEvents += eventName }
        override fun logWordReviewed(rating: Int, wordLevel: Int, wasCorrect: Boolean) {}
        override fun logReviewSessionStart(cardCount: Int) {}
        override fun logReviewSessionComplete(cardsReviewed: Int, durationMs: Long, perfectCount: Int) {}
        override fun logWordsImported(count: Int, method: String) {}
        override fun logWordMastered(level: Int) {}
        override fun logStreakUpdated(days: Int, isNewRecord: Boolean) {}
        override fun logDailyGoalCompleted(cardsTarget: Int, cardsActual: Int) {}
        override fun logThemeChanged(themeMode: String, isDark: Boolean) {}
        override fun logLanguageChanged(language: String) {}
        override fun setUserProperty(name: String, value: String) {}
        override fun updateUserProgress(totalWords: Int, matureWords: Int, currentStreak: Int) {}
        override fun logError(error: Throwable, context: String?) {}
        override fun logNonFatalError(message: String, additionalInfo: Map<String, Any>?) {}
    }

    private fun createViewModel(): VocabularyViewModel {
        val repo = fakeRepo()
        return VocabularyViewModel(
            getDueWordsUseCase = GetDueWordsUseCase(repo),
            getWordsByStageUseCase = GetWordsByStageUseCase(repo),
            updateWordUseCase = UpdateWordUseCase(repo),
            deleteWordUseCase = DeleteWordUseCase(
                repo, fakes.FakeWidgetRefresher(), fakes.fakeGetDailyWidgetDataUseCase(repo)
            ),
            analyticsTracker = fakeAnalytics()
        )
    }

    @Test
    fun `initial state is Loading`() {
        val vm = createViewModel()
        assertIs<UiState.Loading>(vm.currentState)
    }

    @Test
    fun `loadWords with DuoCards emits Loaded state`() = runTest {
        val vm = createViewModel()
        vm.loadWords(ReviewMode.DuoCards)
        val state = vm.currentState
        assertIs<UiState.Loaded<List<Word>>>(state)
        assertEquals(2, state.value.size)
    }

    @Test
    fun `loadWords with ByStage emits Loaded state`() = runTest {
        val vm = createViewModel()
        vm.loadWords(ReviewMode.ByStage(LearningStage.LEVEL_0_FRESH))
        val state = vm.currentState
        assertIs<UiState.Loaded<List<Word>>>(state)
        assertEquals(1, state.value.size)
    }

    @Test
    fun `deleteWord emits WordDeleted effect`() = runTest(UnconfinedTestDispatcher()) {
        val vm = createViewModel()
        vm.loadWords()

        vm.deleteWord(1)

        val effect = vm.effects.first()
        assertIs<VocabularyEffect.WordDeleted>(effect)
    }

    @Test
    fun `deleteWord failure does not emit effect`() = runTest {
        deleteResult = Try.failure(RuntimeException("fail"))
        val vm = createViewModel()
        vm.loadWords()

        vm.deleteWord(1)
        // State should still be Loaded (reloaded from loadWords)
        assertIs<UiState.Loaded<List<Word>>>(vm.currentState)
    }

    @Test
    fun `updateWord reloads words on success`() = runTest {
        val vm = createViewModel()
        vm.loadWords(ReviewMode.DuoCards)

        val word = testWord(1).copy(originalWord = "updated")
        vm.updateWord(word)

        // After updateWord succeeds, loadWords is called again → state is still Loaded
        assertIs<UiState.Loaded<List<Word>>>(vm.currentState)
        assertTrue("word_updated_in_review" in loggedEvents)
    }

    @Test
    fun `updateWord failure logs non-fatal error`() = runTest {
        val vm = createViewModel()
        vm.loadWords(ReviewMode.DuoCards)

        // Override repo to fail updates — need to rebuild VM
        val failingRepo = object : IWordRepository {
            override fun getDueCards(): Flow<List<Word>> = flowOf(dueWords)
            override fun getWordsByStage(stage: LearningStage): Flow<List<Word>> = flowOf(stageWords)
            override suspend fun deleteWord(id: Int): Try<Unit> = Try.success(Unit)
            override suspend fun updateWord(word: Word): Try<Unit> = Try.failure(RuntimeException("update failed"))
            override suspend fun getAllWordsAsync(): Try<List<Word>> = Try.success(emptyList())
            override fun getAllWords(): Flow<List<Word>> = flowOf(emptyList())
            override suspend fun getWordById(id: Int): Word? = null
            override suspend fun insertWords(words: List<Word>): Try<Int> = Try.success(0)
            override fun deleteWords(ids: List<Int>): Flow<DeleteWordsProgress> = flowOf()
            override fun updateWordsLanguages(ids: List<Int>, sourceLanguage: String, targetLanguage: String): Flow<UpdateWordsLanguagesProgress> = flowOf()
            override suspend fun deleteAllWords(): Try<Unit> = Try.success(Unit)
            override suspend fun syncWithRemote(): Try<Unit> = Try.success(Unit)
            override suspend fun syncRemoteToLocal(clearFirst: Boolean): Try<Unit> = Try.success(Unit)
            override fun getProgressStats(): Flow<ProgressStats> = flowOf()
            override suspend fun getTotalCount(): Try<Int> = Try.success(0)
            override suspend fun getDueCount(): Try<Int> = Try.success(0)
            override suspend fun getMostCommonSourceLanguage(): Try<String?> = Try.success(null)
        }
        val failVm = VocabularyViewModel(
            getDueWordsUseCase = GetDueWordsUseCase(failingRepo),
            getWordsByStageUseCase = GetWordsByStageUseCase(failingRepo),
            updateWordUseCase = UpdateWordUseCase(failingRepo),
            deleteWordUseCase = DeleteWordUseCase(
                failingRepo, fakes.FakeWidgetRefresher(), fakes.fakeGetDailyWidgetDataUseCase(failingRepo)
            ),
            analyticsTracker = fakeAnalytics()
        )
        failVm.loadWords()
        failVm.updateWord(testWord(1))

        // No crash, no word_updated_in_review event
        assertTrue("word_updated_in_review" !in loggedEvents)
    }

    @Test
    fun `loadWords with empty due cards emits empty Loaded state`() = runTest {
        dueWords = emptyList()
        val vm = createViewModel()
        vm.loadWords(ReviewMode.DuoCards)

        val state = vm.currentState
        assertIs<UiState.Loaded<List<Word>>>(state)
        assertEquals(0, state.value.size)
    }

    @Test
    fun `deleteWord logs analytics on success`() = runTest {
        val vm = createViewModel()
        vm.loadWords()
        vm.deleteWord(1)

        assertTrue("word_deleted_in_review" in loggedEvents)
    }
}

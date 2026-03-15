package presentation.viewmodel

import analytics.IAnalyticsTracker
import core.common.Try
import domain.auth.model.FeatureAccessResponse
import domain.auth.model.FeatureFlags
import domain.auth.model.UserFeatureAccess
import domain.auth.repository.IAuthRepository
import domain.auth.model.AuthUser
import domain.auth.usecase.GetFeatureAccessUseCase
import domain.word.model.LearningStage
import domain.word.model.ProgressStats
import domain.word.model.Word
import domain.word.repository.DeleteWordsProgress
import domain.word.repository.IWordRepository
import domain.word.repository.UpdateWordsLanguagesProgress
import domain.word.usecase.BatchUpdateLanguagesUseCase
import domain.word.usecase.DeleteWordsUseCase
import domain.word.usecase.ExportWordsUseCase
import domain.word.usecase.GetAllWordsUseCase
import domain.word.usecase.UpdateWordUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import presentation.ViewModelTestBase
import feature.words.model.WordSortOption
import feature.words.WordManagerViewModel
import utils.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class WordManagerViewModelTest : ViewModelTestBase() {

    private fun testWord(id: Int, original: String = "word$id", language: Language = Language.ENGLISH) = Word(
        id = id,
        originalWord = original,
        translation = "trans$id",
        description = "desc$id",
        sourceLanguage = language,
        targetLanguage = Language.GERMAN,
        nextReviewDate = 0L
    )

    private val wordsFlow = MutableStateFlow(listOf(testWord(1), testWord(2), testWord(3)))

    private fun fakeWordRepo() = object : IWordRepository {
        override fun getAllWords(): Flow<List<Word>> = wordsFlow
        override fun getDueCards(): Flow<List<Word>> = flowOf(emptyList())
        override fun getWordsByStage(stage: LearningStage): Flow<List<Word>> = flowOf(emptyList())
        override suspend fun getWordById(id: Int): Word? = null
        override suspend fun getAllWordsAsync(): Try<List<Word>> = Try.success(wordsFlow.value)
        override suspend fun insertWords(words: List<Word>): Try<Int> = Try.success(words.size)
        override suspend fun updateWord(word: Word): Try<Unit> = Try.success(Unit)
        override suspend fun deleteWord(id: Int): Try<Unit> = Try.success(Unit)
        override fun deleteWords(ids: List<Int>): Flow<DeleteWordsProgress> = flowOf(DeleteWordsProgress.Completed(ids.size))
        override fun updateWordsLanguages(ids: List<Int>, sourceLanguage: String, targetLanguage: String): Flow<UpdateWordsLanguagesProgress> =
            flowOf(UpdateWordsLanguagesProgress.Completed(ids.size))
        override suspend fun deleteAllWords(): Try<Unit> = Try.success(Unit)
        override suspend fun syncWithRemote(): Try<Unit> = Try.success(Unit)
        override suspend fun syncRemoteToLocal(clearFirst: Boolean): Try<Unit> = Try.success(Unit)
        override fun getProgressStats(): Flow<ProgressStats> = flowOf(ProgressStats())
        override suspend fun getTotalCount(): Try<Int> = Try.success(0)
        override suspend fun getDueCount(): Try<Int> = Try.success(0)
        override suspend fun getMostCommonSourceLanguage(): Try<String?> = Try.success(null)
    }

    private fun fakeAuthRepo() = object : IAuthRepository {
        override suspend fun loginWithGoogle(idToken: String): Try<AuthUser> = Try.failure(RuntimeException(""))
        override suspend fun loginWithApple(idToken: String, fullName: String?, appleUserId: String): Try<AuthUser> = Try.failure(RuntimeException(""))
        override suspend fun logout(): Try<Unit> = Try.success(Unit)
        override suspend fun deleteAccount(): Try<Unit> = Try.success(Unit)
        override suspend fun getAccessToken(): String? = null
        override suspend fun isAuthenticated(): Boolean = false
        override fun isAuthenticatedAsFlow(): Flow<Boolean> = flowOf(false)
        override fun getFeatureAccessAsFlow(): Flow<FeatureAccessResponse> = flowOf(
            FeatureAccessResponse(FeatureFlags(), UserFeatureAccess(hasPremiumAccess = false))
        )
    }

    private fun fakeAnalytics() = object : IAnalyticsTracker {
        override fun logScreenView(screenName: String) {}
        override fun logEvent(eventName: String, parameters: Map<String, Any>?) {}
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

    private fun createViewModel(): WordManagerViewModel {
        val wordRepo = fakeWordRepo()
        return WordManagerViewModel(
            getAllWordsUseCase = GetAllWordsUseCase(wordRepo),
            deleteWordsUseCase = DeleteWordsUseCase(
                wordRepo, fakes.FakeWidgetRefresher(), fakes.fakeGetDailyWidgetDataUseCase(wordRepo)
            ),
            batchUpdateLanguagesUseCase = BatchUpdateLanguagesUseCase(wordRepo),
            updateWordUseCase = UpdateWordUseCase(wordRepo),
            exportWordsUseCase = ExportWordsUseCase(),
            getFeatureAccessUseCase = GetFeatureAccessUseCase(fakeAuthRepo()),
            analyticsTracker = fakeAnalytics()
        )
    }

    @Test
    fun `initial state has empty selection`() {
        val vm = createViewModel()
        assertEquals(emptySet(), vm.currentState.selectedWordIds)
        assertFalse(vm.currentState.isSelectionMode)
    }

    @Test
    fun `words are loaded from repository`() = runTest {
        val vm = createViewModel()
        assertEquals(3, vm.currentState.words.size)
        assertFalse(vm.currentState.isLoading)
    }

    @Test
    fun `toggleWordSelection adds and removes word`() {
        val vm = createViewModel()
        vm.toggleWordSelection(1)
        assertEquals(setOf(1), vm.currentState.selectedWordIds)
        assertTrue(vm.currentState.isSelectionMode)

        vm.toggleWordSelection(1)
        assertEquals(emptySet(), vm.currentState.selectedWordIds)
        assertFalse(vm.currentState.isSelectionMode)
    }

    @Test
    fun `selectAll selects all filtered words`() = runTest {
        val vm = createViewModel()
        vm.selectAll()
        assertEquals(setOf(1, 2, 3), vm.currentState.selectedWordIds)
        assertTrue(vm.currentState.isSelectionMode)
    }

    @Test
    fun `selectAll toggles off when all selected`() = runTest {
        val vm = createViewModel()
        vm.selectAll()
        vm.selectAll()
        assertEquals(emptySet(), vm.currentState.selectedWordIds)
    }

    @Test
    fun `updateSearchQuery updates state`() {
        val vm = createViewModel()
        vm.updateSearchQuery("test")
        assertEquals("test", vm.currentState.searchQuery)
    }

    @Test
    fun `clearSearch resets query`() {
        val vm = createViewModel()
        vm.updateSearchQuery("test")
        vm.clearSearch()
        assertEquals("", vm.currentState.searchQuery)
    }

    @Test
    fun `setSortOption updates state`() {
        val vm = createViewModel()
        vm.setSortOption(WordSortOption.ALPHABETICAL_AZ)
        assertEquals(WordSortOption.ALPHABETICAL_AZ, vm.currentState.sortOption)
    }

    @Test
    fun `setFilterLanguage updates state`() {
        val vm = createViewModel()
        vm.setFilterLanguage(Language.GERMAN)
        assertEquals(Language.GERMAN, vm.currentState.filterLanguage)
    }

    @Test
    fun `setFilterLearningStage updates state`() {
        val vm = createViewModel()
        vm.setFilterLearningStage(LearningStage.LEVEL_0_FRESH)
        assertEquals(LearningStage.LEVEL_0_FRESH, vm.currentState.filterLearningStage)
    }

    @Test
    fun `enterSelectionMode enables selection`() {
        val vm = createViewModel()
        vm.enterSelectionMode()
        assertTrue(vm.currentState.isSelectionMode)
    }

    @Test
    fun `exitSelectionMode clears selection`() {
        val vm = createViewModel()
        vm.toggleWordSelection(1)
        vm.exitSelectionMode()
        assertFalse(vm.currentState.isSelectionMode)
        assertEquals(emptySet(), vm.currentState.selectedWordIds)
    }

    @Test
    fun `resetState clears all transient state`() {
        val vm = createViewModel()
        vm.toggleWordSelection(1)
        vm.updateSearchQuery("test")

        vm.resetState()

        assertEquals(emptySet(), vm.currentState.selectedWordIds)
        assertFalse(vm.currentState.isSelectionMode)
        assertEquals("", vm.currentState.searchQuery)
    }
}

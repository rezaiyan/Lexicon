package presentation.viewmodel

import domain.word.add.model.AddWordsOutcome
import core.common.NoParamUseCase
import core.common.Try
import domain.onboarding.model.OnboardingPreferences
import domain.onboarding.model.SuggestedVocabulary
import domain.onboarding.repository.IOnboardingRepository
import domain.word.add.usecase.AddStarterWordsUseCase
import domain.word.add.usecase.AddWordsUseCase
import fakes.FakeAddWordsLanguageRepository
import kotlin.time.Clock
import domain.startup.usecase.DetermineAppStartupStateUseCase
import domain.startup.usecase.DeterminePostAuthDestinationUseCase
import domain.word.model.LearningStage
import domain.word.model.ProgressStats
import domain.word.model.Word
import domain.word.repository.DeleteWordsProgress
import domain.word.repository.IWordRepository
import domain.word.repository.UpdateWordsLanguagesProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import presentation.ViewModelTestBase
import feature.auth.AuthPhase
import presentation.model.AppUiState
import utils.Language
import domain.word.add.usecase.UploadPendingWordsUseCase
import fakes.FakeAnalyticsTracker
import fakes.FakeWordRepository
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertEquals

class AppNavigationViewModelTest : ViewModelTestBase() {

    private var insertedWords: List<Word> = emptyList()
    private var onboardingMarkedComplete = false

    private class FakeRetryAnalyticsSyncUseCase : NoParamUseCase<Unit> {
        var called = false
        override suspend fun invoke(params: Unit): Try<Unit> {
            called = true
            return Try.success(Unit)
        }
    }

    private fun fakeOnboardingRepo(
        hasCompleted: Boolean = true
    ) = object : IOnboardingRepository {
        var markCompletedCalled = false
        override suspend fun submitPreferences(preferences: OnboardingPreferences): Try<List<SuggestedVocabulary>> =
            Try.failure(UnsupportedOperationException())
        override suspend fun hasCompletedOnboarding(): Try<Boolean> = Try.success(hasCompleted)
        override suspend fun markOnboardingCompleted(): Try<Unit> {
            markCompletedCalled = true
            onboardingMarkedComplete = true
            return Try.success(Unit)
        }
        override suspend fun resetOnboarding(): Try<Unit> = Try.success(Unit)
    }

    private fun fakeWordRepo(
        totalCount: Int = 0
    ) = object : IWordRepository {
        override suspend fun getTotalCount(): Try<Int> = Try.success(totalCount)
        override suspend fun getAllWordsAsync(): Try<List<Word>> = Try.success(emptyList())
        override fun getAllWords(): Flow<List<Word>> = flowOf(emptyList())
        override fun getDueCards(): Flow<List<Word>> = flowOf(emptyList())
        override fun getDueCardsByTag(tagId: Long): Flow<List<Word>> = flowOf(emptyList())
        override fun getWordsByStage(stage: LearningStage): Flow<List<Word>> = flowOf(emptyList())
        override suspend fun getWordById(id: Int): Word? = null
        override suspend fun addWords(words: List<Word>): Try<AddWordsOutcome> {
            insertedWords = words
            return Try.success(AddWordsOutcome(words.size, 0))
        }
        override suspend fun uploadPendingWords(): Try<Int> = Try.success(0)
        override suspend fun insertWords(words: List<Word>): Try<Int> {
            insertedWords = words
            return Try.success(words.size)
        }
        override suspend fun updateWord(word: Word): Try<Unit> = Try.success(Unit)
        override suspend fun deleteWord(id: Int): Try<Unit> = Try.success(Unit)
        override fun deleteWords(ids: List<Int>): Flow<DeleteWordsProgress> = flowOf(DeleteWordsProgress.Completed(0))
        override fun updateWordsLanguages(
            ids: List<Int>,
            sourceLanguage: String,
            targetLanguage: String,
        ): Flow<UpdateWordsLanguagesProgress> =
            flowOf(UpdateWordsLanguagesProgress.Completed(0))
        override suspend fun deleteAllWords(): Try<Unit> = Try.success(Unit)
        override suspend fun syncWithRemote(): Try<Unit> = Try.success(Unit)
        override suspend fun syncRemoteToLocal(clearFirst: Boolean): Try<Unit> = Try.success(Unit)
        override fun getProgressStats(): Flow<ProgressStats> = flowOf(ProgressStats())
        override suspend fun getDueCount(): Try<Int> = Try.success(0)
        override suspend fun getNextDueAt(): Try<Long?> = Try.success(null)
        override suspend fun getMostCommonSourceLanguage(): Try<String?> = Try.success(null)
        override suspend fun updateWordLocal(word: Word): Try<Unit> = Try.success(Unit)
        override suspend fun batchSyncWords(words: List<Word>): Try<Unit> = Try.success(Unit)
    }

    private val analytics = FakeAnalyticsTracker()

    private fun createViewModel(
        hasCompleted: Boolean = true,
        totalWordCount: Int = 0,
        retryAnalyticsSyncUseCase: NoParamUseCase<Unit> = FakeRetryAnalyticsSyncUseCase(),
        wordRepo: IWordRepository = fakeWordRepo(totalWordCount),
    ): AppNavigationViewModel {
        val onboardingRepo = fakeOnboardingRepo(hasCompleted)
        return AppNavigationViewModel(
            onboardingRepository = onboardingRepo,
            retryAnalyticsSyncUseCase = retryAnalyticsSyncUseCase,
            determineAppStartupStateUseCase = DetermineAppStartupStateUseCase(onboardingRepo),
            determinePostAuthDestinationUseCase = DeterminePostAuthDestinationUseCase(onboardingRepo, wordRepo),
            addStarterWordsUseCase = AddStarterWordsUseCase(AddWordsUseCase(wordRepo, Clock.System), FakeAddWordsLanguageRepository()),
            uploadPendingWordsUseCase = UploadPendingWordsUseCase(wordRepo),
            analytics = analytics,
        )
    }

    @Test
    fun `onAuthComplete when onboarding was reset and no words goes to Onboarding`() = runTest {
        val vm = createViewModel(hasCompleted = false, totalWordCount = 0)

        vm.onAuthComplete()

        assertIs<AppUiState.Onboarding>(vm.currentState)
        assertEquals(false, onboardingMarkedComplete)
    }

    @Test
    fun `onAuthComplete when onboarding was reset but words exist goes to Ready`() = runTest {
        val vm = createViewModel(hasCompleted = false, totalWordCount = 5)

        vm.onAuthComplete()

        assertIs<AppUiState.Ready>(vm.currentState)
        assertEquals(true, onboardingMarkedComplete)
    }

    @Test
    fun `initial state is Auth with Verifying phase`() {
        val vm = createViewModel()
        val state = assertIs<AppUiState.Auth>(vm.currentState)
        assertEquals(AuthPhase.Verifying, state.phase)
    }

    @Test
    fun `onSessionVerified with authenticated and onboarding completed goes to Ready`() = runTest {
        val vm = createViewModel(hasCompleted = true)
        vm.onSessionVerified(isAuthenticated = true)
        assertIs<AppUiState.Ready>(vm.currentState)
    }

    @Test
    fun `onSessionVerified with not authenticated and onboarding completed goes to Auth LoginRequired`() = runTest {
        val vm = createViewModel(hasCompleted = true)
        vm.onSessionVerified(isAuthenticated = false)
        val state = assertIs<AppUiState.Auth>(vm.currentState)
        assertEquals(AuthPhase.LoginRequired, state.phase)
        assertEquals(false, state.needsOnboardingCheck)
    }

    @Test
    fun `onSessionVerified with auth but onboarding not completed marks completed`() = runTest {
        val onboardingRepo = fakeOnboardingRepo(hasCompleted = false)
        val wordRepo = fakeWordRepo()
        val vm = AppNavigationViewModel(
            onboardingRepository = onboardingRepo,
            retryAnalyticsSyncUseCase = FakeRetryAnalyticsSyncUseCase(),
            determineAppStartupStateUseCase = DetermineAppStartupStateUseCase(onboardingRepo),
            determinePostAuthDestinationUseCase = DeterminePostAuthDestinationUseCase(onboardingRepo, wordRepo),
            addStarterWordsUseCase = AddStarterWordsUseCase(AddWordsUseCase(wordRepo, Clock.System), FakeAddWordsLanguageRepository()),
            uploadPendingWordsUseCase = UploadPendingWordsUseCase(wordRepo),
            analytics = analytics,
        )
        vm.onSessionVerified(isAuthenticated = true)
        assertIs<AppUiState.Ready>(vm.currentState)
        assertEquals(true, onboardingRepo.markCompletedCalled)
    }

    @Test
    fun `onSessionVerified unauthenticated and onboarding not completed goes to Auth LoginRequired with onboarding check`() = runTest {
        val vm = createViewModel(hasCompleted = false)
        vm.onSessionVerified(isAuthenticated = false)
        val state = assertIs<AppUiState.Auth>(vm.currentState)
        assertEquals(AuthPhase.LoginRequired, state.phase)
        assertEquals(true, state.needsOnboardingCheck)
    }

    @Test
    fun `onAuthCompleteCheckingData with existing words marks completed and goes to Ready`() = runTest {
        val onboardingRepo = fakeOnboardingRepo(hasCompleted = false)
        val wordRepo = fakeWordRepo(totalCount = 10)
        val vm = AppNavigationViewModel(
            onboardingRepository = onboardingRepo,
            retryAnalyticsSyncUseCase = FakeRetryAnalyticsSyncUseCase(),
            determineAppStartupStateUseCase = DetermineAppStartupStateUseCase(onboardingRepo),
            determinePostAuthDestinationUseCase = DeterminePostAuthDestinationUseCase(onboardingRepo, wordRepo),
            addStarterWordsUseCase = AddStarterWordsUseCase(AddWordsUseCase(wordRepo, Clock.System), FakeAddWordsLanguageRepository()),
            uploadPendingWordsUseCase = UploadPendingWordsUseCase(wordRepo),
            analytics = analytics,
        )
        vm.onAuthCompleteCheckingData()
        assertIs<AppUiState.Ready>(vm.currentState)
        assertEquals(true, onboardingRepo.markCompletedCalled)
    }

    @Test
    fun `onAuthCompleteCheckingData with no words goes to Onboarding`() = runTest {
        val onboardingRepo = fakeOnboardingRepo(hasCompleted = false)
        val wordRepo = fakeWordRepo(totalCount = 0)
        val vm = AppNavigationViewModel(
            onboardingRepository = onboardingRepo,
            retryAnalyticsSyncUseCase = FakeRetryAnalyticsSyncUseCase(),
            determineAppStartupStateUseCase = DetermineAppStartupStateUseCase(onboardingRepo),
            determinePostAuthDestinationUseCase = DeterminePostAuthDestinationUseCase(onboardingRepo, wordRepo),
            addStarterWordsUseCase = AddStarterWordsUseCase(AddWordsUseCase(wordRepo, Clock.System), FakeAddWordsLanguageRepository()),
            uploadPendingWordsUseCase = UploadPendingWordsUseCase(wordRepo),
            analytics = analytics,
        )
        vm.onAuthCompleteCheckingData()
        assertIs<AppUiState.Onboarding>(vm.currentState)
        assertEquals(false, onboardingRepo.markCompletedCalled)
    }

    @Test
    fun `onLogout goes to Auth LoginRequired`() {
        val vm = createViewModel()
        vm.onLogout()
        val state = assertIs<AppUiState.Auth>(vm.currentState)
        assertEquals(AuthPhase.LoginRequired, state.phase)
    }

    @Test
    fun `onAuthComplete when onboarding already completed goes to Ready`() = runTest {
        val vm = createViewModel(hasCompleted = true)

        vm.onAuthComplete()

        assertIs<AppUiState.Ready>(vm.currentState)
    }

    @Test
    fun `onNavigateToVocabularyPreview sets VocabularyPreview state`() {
        val vm = createViewModel()
        val words = listOf(
            SuggestedVocabulary("hola", "hello", "greeting", Language.ENGLISH, Language.SPANISH)
        )
        vm.onNavigateToVocabularyPreview(words)
        val state = assertIs<AppUiState.VocabularyPreview>(vm.currentState)
        assertEquals(words, state.words)
    }

    @Test
    fun `onOnboardingFinished with words imports them and opens the app`() = runTest {
        val vm = createViewModel(hasCompleted = false)
        val words = listOf(
            SuggestedVocabulary("hola", "hello", "greeting", Language.ENGLISH, Language.SPANISH)
        )

        vm.onOnboardingFinished(words)

        assertIs<AppUiState.Ready>(vm.currentState)
        assertEquals(listOf("hola"), insertedWords.map { it.originalWord })
        assertEquals(Language.SPANISH, insertedWords.single().targetLanguage)
        assertEquals(true, onboardingMarkedComplete)
        assertEquals(listOf(1 to "onboarding"), analytics.wordsImported)
    }

    @Test
    fun `onOnboardingFinished without words imports nothing and opens the app`() = runTest {
        val vm = createViewModel(hasCompleted = false)

        vm.onOnboardingFinished(emptyList())

        assertIs<AppUiState.Ready>(vm.currentState)
        assertEquals(emptyList(), insertedWords)
        assertEquals(true, onboardingMarkedComplete)
        assertEquals(emptyList(), analytics.wordsImported)
    }

    @Test
    fun `isVerifying returns true only in Verifying phase`() {
        val vm = createViewModel()
        assertEquals(true, vm.isVerifying)
        vm.onLogout()
        assertEquals(false, vm.isVerifying)
    }

    @Test
    fun `onSessionVerified authenticated triggers analytics retry`() = runTest {
        val retryUseCase = FakeRetryAnalyticsSyncUseCase()
        val vm = createViewModel(hasCompleted = true, retryAnalyticsSyncUseCase = retryUseCase)
        vm.onSessionVerified(isAuthenticated = true)
        assertEquals(true, retryUseCase.called)
    }

    @Test
    fun `onSessionVerified unauthenticated does not trigger analytics retry`() = runTest {
        val retryUseCase = FakeRetryAnalyticsSyncUseCase()
        val vm = createViewModel(hasCompleted = true, retryAnalyticsSyncUseCase = retryUseCase)
        vm.onSessionVerified(isAuthenticated = false)
        assertEquals(false, retryUseCase.called)
    }

    @Test
    fun `onSessionVerified authenticated uploads words added while offline`() = runTest {
        val words = FakeWordRepository()
        val vm = createViewModel(hasCompleted = true, wordRepo = words)

        vm.onSessionVerified(isAuthenticated = true)

        assertEquals(1, words.uploadCallCount)
    }

    @Test
    fun `onSessionVerified unauthenticated does not upload words`() = runTest {
        val words = FakeWordRepository()
        val vm = createViewModel(hasCompleted = true, wordRepo = words)

        vm.onSessionVerified(isAuthenticated = false)

        assertEquals(0, words.uploadCallCount)
    }
}

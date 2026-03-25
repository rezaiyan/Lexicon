package presentation.viewmodel

import core.common.Try
import domain.analytics.repository.IAnalyticsRecorder
import domain.analytics.usecase.RetryAnalyticsSyncUseCase
import domain.onboarding.model.OnboardingPreferences
import domain.onboarding.model.SuggestedVocabulary
import domain.onboarding.model.SuggestedVocabularyResponse
import domain.onboarding.repository.IOnboardingRepository
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
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertEquals

class AppNavigationViewModelTest : ViewModelTestBase() {

    private fun fakeOnboardingRepo(
        hasCompleted: Boolean = true
    ) = object : IOnboardingRepository {
        var markCompletedCalled = false
        override suspend fun submitPreferences(preferences: OnboardingPreferences): Try<SuggestedVocabularyResponse> =
            Try.failure(UnsupportedOperationException())
        override suspend fun hasCompletedOnboarding(): Try<Boolean> = Try.success(hasCompleted)
        override suspend fun markOnboardingCompleted(): Try<Unit> {
            markCompletedCalled = true
            return Try.success(Unit)
        }
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
        override suspend fun insertWords(words: List<Word>): Try<Int> = Try.success(0)
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
        override suspend fun getMostCommonSourceLanguage(): Try<String?> = Try.success(null)
    }

    private class FakeAnalyticsRecorder : IAnalyticsRecorder {
        var retryCalled = false
        override suspend fun startSession(sessionId: String, reviewType: String, startedAt: Long) = Try.success(Unit)
        override suspend fun endSession(sessionId: String, endedAt: Long, durationMs: Long, totalCards: Int, correctCount: Int, incorrectCount: Int, completedNormally: Boolean) = Try.success(Unit)
        override suspend fun recordReviewEvent(params: domain.analytics.model.ReviewEventParams) = Try.success(Unit)
        override suspend fun retryPendingSync(): Try<Unit> {
            retryCalled = true
            return Try.success(Unit)
        }
    }

    private fun fakeRetryUseCase(recorder: FakeAnalyticsRecorder = FakeAnalyticsRecorder()) =
        RetryAnalyticsSyncUseCase(recorder)

    private fun createViewModel(
        hasCompleted: Boolean = true,
        totalWordCount: Int = 0,
        retryUseCase: RetryAnalyticsSyncUseCase = fakeRetryUseCase(),
    ): AppNavigationViewModel {
        return AppNavigationViewModel(fakeOnboardingRepo(hasCompleted), fakeWordRepo(totalWordCount), retryUseCase)
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
    fun `onSessionVerified with auth but onboarding not completed marks completed`() =
        runTest {
            val onboardingRepo = fakeOnboardingRepo(hasCompleted = false)
            val vm = AppNavigationViewModel(onboardingRepo, fakeWordRepo(), fakeRetryUseCase())
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
        val vm = AppNavigationViewModel(onboardingRepo, fakeWordRepo(totalCount = 10), fakeRetryUseCase())
        vm.onAuthCompleteCheckingData()
        assertIs<AppUiState.Ready>(vm.currentState)
        assertEquals(true, onboardingRepo.markCompletedCalled)
    }

    @Test
    fun `onAuthCompleteCheckingData with no words goes to Onboarding`() = runTest {
        val onboardingRepo = fakeOnboardingRepo(hasCompleted = false)
        val vm = AppNavigationViewModel(onboardingRepo, fakeWordRepo(totalCount = 0), fakeRetryUseCase())
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
    fun `onAuthComplete marks onboarding completed and goes to Ready`() = runTest {
        val repo = fakeOnboardingRepo()
        val vm = AppNavigationViewModel(repo, fakeWordRepo(), fakeRetryUseCase())
        vm.onAuthComplete()
        assertIs<AppUiState.Ready>(vm.currentState)
        assertEquals(true, repo.markCompletedCalled)
    }

    @Test
    fun `onNavigateToVocabularyPreview sets VocabularyPreview state`() {
        val vm = createViewModel()
        val words = listOf(
            SuggestedVocabulary("hola", "hello", "greeting", "es", "en")
        )
        vm.onNavigateToVocabularyPreview(words)
        val state = assertIs<AppUiState.VocabularyPreview>(vm.currentState)
        assertEquals(words, state.words)
    }

    @Test
    fun `onNavigateToAuthGate with pending vocabulary passes words`() {
        val vm = createViewModel()
        val words = listOf(
            SuggestedVocabulary("hola", "hello", "greeting", "es", "en")
        )
        vm.onNavigateToAuthGate(words)
        val state = assertIs<AppUiState.Auth>(vm.currentState)
        assertEquals(AuthPhase.LoginRequired, state.phase)
        assertEquals(words, state.pendingVocabulary)
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
        val recorder = FakeAnalyticsRecorder()
        val vm = createViewModel(retryUseCase = fakeRetryUseCase(recorder))
        vm.onSessionVerified(isAuthenticated = true)
        assertEquals(true, recorder.retryCalled)
    }

    @Test
    fun `onSessionVerified unauthenticated does not trigger analytics retry`() = runTest {
        val recorder = FakeAnalyticsRecorder()
        val vm = createViewModel(retryUseCase = fakeRetryUseCase(recorder))
        vm.onSessionVerified(isAuthenticated = false)
        assertEquals(false, recorder.retryCalled)
    }
}

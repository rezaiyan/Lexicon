package presentation.viewmodel

import core.common.Try
import domain.onboarding.model.OnboardingPreferences
import domain.onboarding.model.SuggestedVocabulary
import domain.onboarding.model.SuggestedVocabularyResponse
import domain.onboarding.repository.IOnboardingRepository
import kotlinx.coroutines.test.runTest
import presentation.ViewModelTestBase
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
        override suspend fun hasCompletedOnboarding(): Boolean = hasCompleted
        override suspend fun markOnboardingCompleted() { markCompletedCalled = true }
    }

    private fun createViewModel(hasCompleted: Boolean = true) =
        AppNavigationViewModel(fakeOnboardingRepo(hasCompleted))

    @Test
    fun `initial state is Splash`() {
        val vm = createViewModel()
        assertIs<AppUiState.Splash>(vm.currentState)
    }

    @Test
    fun `onSplashComplete with authenticated and onboarding completed goes to Ready`() = runTest {
        val vm = createViewModel(hasCompleted = true)
        vm.onSplashComplete(isAuthenticated = true)
        assertIs<AppUiState.Ready>(vm.currentState)
    }

    @Test
    fun `onSplashComplete with not authenticated and onboarding completed goes to AuthGate`() = runTest {
        val vm = createViewModel(hasCompleted = true)
        vm.onSplashComplete(isAuthenticated = false)
        assertIs<AppUiState.AuthGate>(vm.currentState)
    }

    @Test
    fun `onSplashComplete with onboarding not completed goes to Onboarding`() = runTest {
        val vm = createViewModel(hasCompleted = false)
        vm.onSplashComplete(isAuthenticated = true)
        assertIs<AppUiState.Onboarding>(vm.currentState)
    }

    @Test
    fun `onLogout goes to AuthGate`() {
        val vm = createViewModel()
        vm.onLogout()
        assertIs<AppUiState.AuthGate>(vm.currentState)
    }

    @Test
    fun `onAuthComplete marks onboarding completed and goes to Ready`() = runTest {
        val repo = fakeOnboardingRepo()
        val vm = AppNavigationViewModel(repo)
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
        val state = assertIs<AppUiState.AuthGate>(vm.currentState)
        assertEquals(words, state.pendingVocabulary)
    }

    @Test
    fun `isSplash returns true only in Splash state`() {
        val vm = createViewModel()
        assertEquals(true, vm.isSplash)
        vm.onLogout()
        assertEquals(false, vm.isSplash)
    }
}

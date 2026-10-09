package presentation.viewmodel

import analytics.IAnalyticsTracker
import androidx.lifecycle.viewModelScope
import core.common.NoParamUseCase
import core.common.fold
import core.common.getOrDefault
import core.common.onSuccess
import domain.onboarding.model.SuggestedVocabulary
import domain.onboarding.repository.IOnboardingRepository
import domain.word.add.model.WordOrigin
import domain.startup.model.AppStartupDestination
import domain.word.add.usecase.AddStarterWordsUseCase
import domain.word.add.usecase.UploadPendingWordsUseCase
import domain.startup.usecase.DetermineAppStartupStateUseCase
import domain.startup.usecase.DeterminePostAuthDestinationUseCase
import kotlinx.coroutines.launch
import core.base.BaseViewModel
import feature.auth.AuthPhase
import presentation.model.AppUiState

class AppNavigationViewModel(
    private val onboardingRepository: IOnboardingRepository,
    private val retryAnalyticsSyncUseCase: NoParamUseCase<Unit>,
    private val determineAppStartupStateUseCase: DetermineAppStartupStateUseCase,
    private val determinePostAuthDestinationUseCase: DeterminePostAuthDestinationUseCase,
    private val addStarterWordsUseCase: AddStarterWordsUseCase,
    private val uploadPendingWordsUseCase: UploadPendingWordsUseCase,
    private val analytics: IAnalyticsTracker,
) : BaseViewModel<AppUiState, Nothing>() {

    override fun initialState(): AppUiState = AppUiState.Auth()

    /** Non-composable read for Android splash screen keep-on-screen condition. */
    val isVerifying: Boolean get() = (currentState as? AppUiState.Auth)?.phase == AuthPhase.Verifying

    fun onSessionVerified(isAuthenticated: Boolean) {
        // Retry what failed to reach the server in a previous run (analytics, words added offline) — fire-and-forget.
        if (isAuthenticated) {
            viewModelScope.launch { retryAnalyticsSyncUseCase(Unit) }
            viewModelScope.launch { uploadPendingWordsUseCase(Unit) }
        }
        viewModelScope.launch {
            determineAppStartupStateUseCase(isAuthenticated).fold(
                onSuccess = { destination -> updateState { destination.toAppUiState() } },
                onFailure = { updateState { AppUiState.Auth(phase = AuthPhase.LoginRequired) } }
            )
        }
    }

    fun onNavigateToVocabularyPreview(words: List<SuggestedVocabulary>) {
        updateState { AppUiState.VocabularyPreview(words) }
    }

    /**
     * Ends onboarding (the user is already signed in): adds the accepted starter [words], if any,
     * marks onboarding done and opens the app. A failed import doesn't block entry; the list starts empty.
     */
    fun onOnboardingFinished(words: List<SuggestedVocabulary>) {
        viewModelScope.launch {
            addStarterWordsUseCase(words).onSuccess { outcome ->
                if (outcome.added > 0) analytics.logWordsImported(outcome.added, WordOrigin.Onboarding.analyticsName)
            }
            onboardingRepository.markOnboardingCompleted()
            updateState { AppUiState.Ready }
        }
    }

    /**
     * Called after login when onboarding was already done on this device. If the flag has since been
     * cleared (the account was deleted), falls back to the returning-user check so onboarding can run again.
     */
    fun onAuthComplete() {
        viewModelScope.launch {
            val completed = onboardingRepository.hasCompletedOnboarding().getOrDefault(false)
            if (completed) {
                updateState { AppUiState.Ready }
            } else {
                routeAfterAuthByExistingData()
            }
        }
    }

    /**
     * Called after login when we need to check if the user has existing data.
     * If they have words (returning user), skip onboarding. Otherwise show it.
     */
    fun onAuthCompleteCheckingData() {
        viewModelScope.launch { routeAfterAuthByExistingData() }
    }

    private suspend fun routeAfterAuthByExistingData() {
        determinePostAuthDestinationUseCase(Unit).fold(
            onSuccess = { destination -> updateState { destination.toAppUiState() } },
            onFailure = { updateState { AppUiState.Onboarding } }
        )
    }

    fun onLogout() {
        updateState { AppUiState.Auth(phase = AuthPhase.LoginRequired) }
    }

    private fun AppStartupDestination.toAppUiState(): AppUiState = when (this) {
        is AppStartupDestination.Ready -> AppUiState.Ready
        is AppStartupDestination.Onboarding -> AppUiState.Onboarding
        is AppStartupDestination.RequiresAuth -> AppUiState.Auth(
            phase = AuthPhase.LoginRequired,
            needsOnboardingCheck = needsOnboardingCheck,
        )
    }
}

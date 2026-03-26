package presentation.viewmodel

import androidx.lifecycle.viewModelScope
import core.common.NoParamUseCase
import core.common.fold
import domain.onboarding.model.SuggestedVocabulary
import domain.onboarding.repository.IOnboardingRepository
import domain.startup.model.AppStartupDestination
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
) : BaseViewModel<AppUiState, Nothing>() {

    override fun initialState(): AppUiState = AppUiState.Auth()

    /** Non-composable read for Android splash screen keep-on-screen condition. */
    val isVerifying: Boolean get() = (currentState as? AppUiState.Auth)?.phase == AuthPhase.Verifying

    fun onSessionVerified(isAuthenticated: Boolean) {
        // Retry any sessions that failed to sync in a previous run — fire-and-forget.
        if (isAuthenticated) {
            viewModelScope.launch { retryAnalyticsSyncUseCase(Unit) }
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

    fun onNavigateToAuthGate(pendingVocabulary: List<SuggestedVocabulary> = emptyList()) {
        updateState { AppUiState.Auth(phase = AuthPhase.LoginRequired, pendingVocabulary = pendingVocabulary) }
    }

    fun onAuthComplete() {
        viewModelScope.launch {
            onboardingRepository.markOnboardingCompleted()
            updateState { AppUiState.Ready }
        }
    }

    /**
     * Called after login when we need to check if the user has existing data.
     * If they have words (returning user), skip onboarding. Otherwise show it.
     */
    fun onAuthCompleteCheckingData() {
        viewModelScope.launch {
            determinePostAuthDestinationUseCase(Unit).fold(
                onSuccess = { destination -> updateState { destination.toAppUiState() } },
                onFailure = { updateState { AppUiState.Onboarding } }
            )
        }
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

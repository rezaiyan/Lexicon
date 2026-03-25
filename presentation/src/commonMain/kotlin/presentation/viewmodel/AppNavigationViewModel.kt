package presentation.viewmodel

import androidx.lifecycle.viewModelScope
import domain.analytics.usecase.RetryAnalyticsSyncUseCase
import domain.onboarding.model.SuggestedVocabulary
import domain.onboarding.repository.IOnboardingRepository
import domain.word.repository.IWordRepository
import core.common.getOrDefault
import core.common.getOrElse
import kotlinx.coroutines.launch
import core.base.BaseViewModel
import feature.auth.AuthPhase
import presentation.model.AppUiState

class AppNavigationViewModel(
    private val onboardingRepository: IOnboardingRepository,
    private val wordRepository: IWordRepository,
    private val retryAnalyticsSyncUseCase: RetryAnalyticsSyncUseCase,
) : BaseViewModel<AppUiState, Nothing>() {

    override fun initialState(): AppUiState = AppUiState.Auth()

    /** Non-composable read for Android splash screen keep-on-screen condition. */
    val isVerifying: Boolean get() = (currentState as? AppUiState.Auth)?.phase == AuthPhase.Verifying

    fun onSessionVerified(isAuthenticated: Boolean) {
        viewModelScope.launch {
            if (isAuthenticated) {
                retryAnalyticsSyncUseCase()
            }

            val onboardingCompleted = onboardingRepository.hasCompletedOnboarding().getOrDefault(false)
            when {
                onboardingCompleted && isAuthenticated -> updateState { AppUiState.Ready }
                onboardingCompleted -> updateState { AppUiState.Auth(phase = AuthPhase.LoginRequired) }
                isAuthenticated -> {
                    // User has a valid session but lost the onboarding flag (e.g. reinstall
                    // with Keychain-persisted tokens). They're a returning user — skip onboarding.
                    onboardingRepository.markOnboardingCompleted()
                    updateState { AppUiState.Ready }
                }
                else -> {
                    // Not authenticated and no onboarding flag — show auth first,
                    // then decide about onboarding based on whether the user has data.
                    updateState { AppUiState.Auth(phase = AuthPhase.LoginRequired, needsOnboardingCheck = true) }
                }
            }
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
            val wordCount = wordRepository.getTotalCount().getOrElse { 0 }
            if (wordCount > 0) {
                onboardingRepository.markOnboardingCompleted()
                updateState { AppUiState.Ready }
            } else {
                updateState { AppUiState.Onboarding }
            }
        }
    }

    fun onLogout() {
        updateState { AppUiState.Auth(phase = AuthPhase.LoginRequired) }
    }
}

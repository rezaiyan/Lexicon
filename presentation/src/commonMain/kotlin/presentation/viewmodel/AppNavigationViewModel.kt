package presentation.viewmodel

import androidx.lifecycle.viewModelScope
import domain.onboarding.model.SuggestedVocabulary
import domain.onboarding.repository.IOnboardingRepository
import domain.word.repository.IWordRepository
import core.common.getOrDefault
import core.common.getOrElse
import kotlinx.coroutines.launch
import core.base.BaseViewModel
import presentation.model.AppUiState

class AppNavigationViewModel(
    private val onboardingRepository: IOnboardingRepository,
    private val wordRepository: IWordRepository
) : BaseViewModel<AppUiState, Nothing>() {

    override fun initialState(): AppUiState = AppUiState.Splash

    /** Non-composable read for Android splash screen keep-on-screen condition. */
    val isSplash: Boolean get() = currentState is AppUiState.Splash

    fun onSplashComplete(isAuthenticated: Boolean) {
        viewModelScope.launch {
            val onboardingCompleted = onboardingRepository.hasCompletedOnboarding().getOrDefault(false)
            when {
                onboardingCompleted && isAuthenticated -> updateState { AppUiState.Ready }
                onboardingCompleted -> updateState { AppUiState.AuthGate() }
                isAuthenticated -> {
                    // User has a valid session but lost the onboarding flag (e.g. reinstall
                    // with Keychain-persisted tokens). They're a returning user — skip onboarding.
                    onboardingRepository.markOnboardingCompleted()
                    updateState { AppUiState.Ready }
                }
                else -> {
                    // Not authenticated and no onboarding flag — show auth first,
                    // then decide about onboarding based on whether the user has data.
                    updateState { AppUiState.AuthGate(needsOnboardingCheck = true) }
                }
            }
        }
    }

    fun onNavigateToVocabularyPreview(words: List<SuggestedVocabulary>) {
        updateState { AppUiState.VocabularyPreview(words) }
    }

    fun onNavigateToAuthGate(pendingVocabulary: List<SuggestedVocabulary> = emptyList()) {
        updateState { AppUiState.AuthGate(pendingVocabulary) }
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
        updateState { AppUiState.AuthGate() }
    }
}

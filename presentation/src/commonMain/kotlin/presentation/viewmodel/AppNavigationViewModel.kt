package presentation.viewmodel

import androidx.lifecycle.viewModelScope
import domain.onboarding.model.SuggestedVocabulary
import domain.onboarding.repository.IOnboardingRepository
import kotlinx.coroutines.launch
import core.base.BaseViewModel
import presentation.model.AppUiState

class AppNavigationViewModel(
    private val onboardingRepository: IOnboardingRepository
) : BaseViewModel<AppUiState, Nothing>() {

    override fun initialState(): AppUiState = AppUiState.Splash

    /** Non-composable read for Android splash screen keep-on-screen condition. */
    val isSplash: Boolean get() = currentState is AppUiState.Splash

    fun onSplashComplete(isAuthenticated: Boolean) {
        viewModelScope.launch {
            val onboardingCompleted = onboardingRepository.hasCompletedOnboarding()
            updateState {
                when {
                    onboardingCompleted && isAuthenticated -> AppUiState.Ready
                    onboardingCompleted -> AppUiState.AuthGate()
                    else -> AppUiState.Onboarding
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

    fun onLogout() {
        updateState { AppUiState.AuthGate() }
    }
}

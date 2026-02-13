package presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import domain.onboarding.model.SuggestedVocabulary
import domain.onboarding.repository.IOnboardingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import presentation.model.AppUiState

class AppNavigationViewModel(
    private val onboardingRepository: IOnboardingRepository
) : ViewModel() {

    private val _appUiState = MutableStateFlow<AppUiState>(AppUiState.Splash)
    val appUiState = _appUiState.asStateFlow()

    fun onSplashComplete(isAuthenticated: Boolean) {
        viewModelScope.launch {
            val onboardingCompleted = onboardingRepository.hasCompletedOnboarding()
            _appUiState.value = when {
                onboardingCompleted && isAuthenticated -> AppUiState.Ready
                onboardingCompleted -> AppUiState.AuthGate()
                else -> AppUiState.Onboarding
            }
        }
    }

    fun onNavigateToVocabularyPreview(words: List<SuggestedVocabulary>) {
        _appUiState.value = AppUiState.VocabularyPreview(words)
    }

    fun onNavigateToAuthGate(pendingVocabulary: List<SuggestedVocabulary> = emptyList()) {
        _appUiState.value = AppUiState.AuthGate(pendingVocabulary)
    }

    fun onAuthComplete() {
        viewModelScope.launch {
            onboardingRepository.markOnboardingCompleted()
            _appUiState.value = AppUiState.Ready
        }
    }
}

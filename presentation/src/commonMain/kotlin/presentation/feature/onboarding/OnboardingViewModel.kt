package presentation.feature.onboarding

import androidx.lifecycle.viewModelScope
import domain.onboarding.model.OnboardingPreferences
import domain.onboarding.model.SuggestedVocabularyResponse
import core.common.onFailure
import core.common.onSuccess
import domain.onboarding.usecase.SubmitPreferencesUseCase
import domain.settings.usecase.SetLanguageUseCase
import utils.Language
import kotlinx.coroutines.launch
import core.base.BaseViewModel
import presentation.model.OnboardingUiState

class OnboardingViewModel(
    private val submitPreferencesUseCase: SubmitPreferencesUseCase,
    private val setLanguageUseCase: SetLanguageUseCase
) : BaseViewModel<OnboardingUiState, OnboardingViewModel.Event>() {

    sealed interface Event {
        data class NavigateToPreview(val response: SuggestedVocabularyResponse) : Event
        data object NavigateToMain : Event
    }

    override fun initialState() = OnboardingUiState()

    fun selectTargetLanguage(language: String) {
        updateState { copy(selectedTargetLanguage = language) }
    }

    fun selectNativeLanguage(language: String) {
        updateState { copy(selectedNativeLanguage = language) }
    }

    fun selectLevel(level: String) {
        updateState { copy(selectedLevel = level) }
    }

    fun nextStep() {
        updateState {
            if (currentStep < totalSteps) {
                copy(currentStep = currentStep + 1, error = null)
            } else this
        }
    }

    fun previousStep() {
        updateState {
            if (currentStep > 1) {
                copy(currentStep = currentStep - 1, error = null)
            } else this
        }
    }

    fun submit() {
        val state = currentState
        val targetLang = state.selectedTargetLanguage ?: return
        val nativeLang = state.selectedNativeLanguage ?: return
        val level = state.selectedLevel ?: return

        viewModelScope.launch {
            updateState { copy(isLoading = true, error = null) }
            val preferences = OnboardingPreferences(
                targetLanguage = targetLang,
                nativeLanguage = nativeLang,
                level = level,
                interests = state.interests
            )
            submitPreferencesUseCase(preferences)
                .onSuccess { response ->
                    setLanguageUseCase(Language.fromCodeOrName(targetLang))
                    updateState { copy(isLoading = false) }
                    emitEffect(Event.NavigateToPreview(response))
                }
                .onFailure { error ->
                    updateState { copy(isLoading = false, error = error.message) }
                }
        }
    }

    fun skip() {
        emitEffect(Event.NavigateToMain)
    }
}

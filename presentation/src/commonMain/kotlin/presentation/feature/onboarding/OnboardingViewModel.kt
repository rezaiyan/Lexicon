package presentation.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import domain.onboarding.model.OnboardingPreferences
import domain.onboarding.model.SuggestedVocabularyResponse
import domain.onboarding.usecase.SubmitPreferencesUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import presentation.model.OnboardingUiState

class OnboardingViewModel(
    private val submitPreferencesUseCase: SubmitPreferencesUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    sealed interface Event {
        data class NavigateToPreview(val response: SuggestedVocabularyResponse) : Event
        data object NavigateToMain : Event
    }

    private val _events = MutableSharedFlow<Event>()
    val events = _events.asSharedFlow()

    fun selectTargetLanguage(language: String) {
        _state.update { it.copy(selectedTargetLanguage = language) }
    }

    fun selectNativeLanguage(language: String) {
        _state.update { it.copy(selectedNativeLanguage = language) }
    }

    fun selectLevel(level: String) {
        _state.update { it.copy(selectedLevel = level) }
    }

    fun submit() {
        val currentState = _state.value
        val targetLang = currentState.selectedTargetLanguage ?: return
        val nativeLang = currentState.selectedNativeLanguage ?: return
        val level = currentState.selectedLevel ?: return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val preferences = OnboardingPreferences(
                targetLanguage = targetLang,
                nativeLanguage = nativeLang,
                level = level,
                interests = currentState.interests
            )
            submitPreferencesUseCase(preferences)
                .onSuccess { response ->
                    _state.update { it.copy(isLoading = false) }
                    _events.emit(Event.NavigateToPreview(response))
                }
                .onFailure { error ->
                    _state.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    fun skip() {
        viewModelScope.launch {
            _events.emit(Event.NavigateToMain)
        }
    }
}

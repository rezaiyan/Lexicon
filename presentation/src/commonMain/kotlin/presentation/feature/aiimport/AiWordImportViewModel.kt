package presentation.feature.aiimport

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import domain.common.onFailure
import domain.common.onSuccess
import domain.onboarding.model.OnboardingPreferences
import domain.onboarding.usecase.ImportSuggestedVocabularyUseCase
import domain.onboarding.usecase.SubmitPreferencesUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import presentation.model.AiWordImportStep
import presentation.model.AiWordImportUiState

class AiWordImportViewModel(
    private val submitPreferencesUseCase: SubmitPreferencesUseCase,
    private val importSuggestedVocabularyUseCase: ImportSuggestedVocabularyUseCase,
) : ViewModel() {

    sealed interface Event {
        data class ImportSuccess(val count: Int) : Event
        data object Dismiss : Event
    }

    private val _state = MutableStateFlow(AiWordImportUiState())
    val state: StateFlow<AiWordImportUiState> = _state.asStateFlow()

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

    fun toggleTopic(topic: String) {
        _state.update { current ->
            val updated = if (current.selectedTopics.contains(topic)) {
                current.selectedTopics - topic
            } else {
                current.selectedTopics + topic
            }
            current.copy(selectedTopics = updated)
        }
    }

    fun toggleWordSelection(index: Int) {
        _state.update { current ->
            val updated = if (current.selectedWordIndices.contains(index)) {
                current.selectedWordIndices - index
            } else {
                current.selectedWordIndices + index
            }
            current.copy(selectedWordIndices = updated)
        }
    }

    fun nextStep() {
        _state.update { current ->
            val next = when (current.step) {
                AiWordImportStep.TARGET_LANG -> AiWordImportStep.NATIVE_LANG
                AiWordImportStep.NATIVE_LANG -> AiWordImportStep.LEVEL
                AiWordImportStep.LEVEL -> AiWordImportStep.TOPICS
                AiWordImportStep.TOPICS -> current.step
                AiWordImportStep.PREVIEW -> current.step
            }
            current.copy(step = next, error = null)
        }
    }

    fun previousStep() {
        _state.update { current ->
            val prev = when (current.step) {
                AiWordImportStep.TARGET_LANG -> current.step
                AiWordImportStep.NATIVE_LANG -> AiWordImportStep.TARGET_LANG
                AiWordImportStep.LEVEL -> AiWordImportStep.NATIVE_LANG
                AiWordImportStep.TOPICS -> AiWordImportStep.LEVEL
                AiWordImportStep.PREVIEW -> AiWordImportStep.TOPICS
            }
            current.copy(step = prev, error = null)
        }
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
                interests = currentState.selectedTopics.toList()
            )
            submitPreferencesUseCase(preferences)
                .onSuccess { response ->
                    val allIndices = response.suggestedVocabulary.indices.toSet()
                    _state.update {
                        it.copy(
                            isLoading = false,
                            step = AiWordImportStep.PREVIEW,
                            suggestedWords = response.suggestedVocabulary,
                            selectedWordIndices = allIndices,
                            error = null
                        )
                    }
                }
                .onFailure { error ->
                    _state.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    fun importSelected() {
        val currentState = _state.value
        val wordsToImport = currentState.selectedWordIndices
            .sorted()
            .mapNotNull { currentState.suggestedWords.getOrNull(it) }

        if (wordsToImport.isEmpty()) return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            importSuggestedVocabularyUseCase(wordsToImport)
                .onSuccess { count ->
                    _state.update { it.copy(isLoading = false) }
                    _events.emit(Event.ImportSuccess(count))
                }
                .onFailure { error ->
                    _state.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    fun dismiss() {
        viewModelScope.launch {
            _events.emit(Event.Dismiss)
        }
    }

    fun reset() {
        _state.value = AiWordImportUiState()
    }
}

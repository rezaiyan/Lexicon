package feature.aiimport

import androidx.lifecycle.viewModelScope
import core.common.onFailure
import core.common.onSuccess
import domain.onboarding.model.OnboardingPreferences
import domain.onboarding.usecase.ImportSuggestedVocabularyUseCase
import domain.onboarding.usecase.SubmitPreferencesUseCase
import kotlinx.coroutines.launch
import core.base.BaseViewModel
import feature.aiimport.model.AiWordImportStep
import feature.aiimport.model.AiWordImportUiState

class AiWordImportViewModel(
    private val submitPreferencesUseCase: SubmitPreferencesUseCase,
    private val importSuggestedVocabularyUseCase: ImportSuggestedVocabularyUseCase,
) : BaseViewModel<AiWordImportUiState, AiWordImportViewModel.Event>() {

    sealed interface Event {
        data class ImportSuccess(val count: Int) : Event
        data object Dismiss : Event
    }

    override fun initialState() = AiWordImportUiState()

    fun selectTargetLanguage(language: String) {
        updateState { copy(selectedTargetLanguage = language) }
        nextStep()
    }

    fun selectNativeLanguage(language: String) {
        updateState { copy(selectedNativeLanguage = language) }
        nextStep()
    }

    fun selectLevel(level: String) {
        updateState { copy(selectedLevel = level) }
    }

    fun toggleTopic(topic: String) {
        updateState {
            val updated = if (selectedTopics.contains(topic)) {
                selectedTopics - topic
            } else {
                selectedTopics + topic
            }
            copy(selectedTopics = updated)
        }
    }

    fun toggleWordSelection(index: Int) {
        updateState {
            val updated = if (selectedWordIndices.contains(index)) {
                selectedWordIndices - index
            } else {
                selectedWordIndices + index
            }
            copy(selectedWordIndices = updated)
        }
    }

    fun nextStep() {
        updateState {
            val next = when (step) {
                AiWordImportStep.TARGET_LANG -> AiWordImportStep.NATIVE_LANG
                AiWordImportStep.NATIVE_LANG -> AiWordImportStep.LEVEL
                AiWordImportStep.LEVEL -> AiWordImportStep.TOPICS
                AiWordImportStep.TOPICS -> step
                AiWordImportStep.PREVIEW -> step
            }
            copy(step = next, error = null)
        }
    }

    fun previousStep() {
        updateState {
            val prev = when (step) {
                AiWordImportStep.TARGET_LANG -> step
                AiWordImportStep.NATIVE_LANG -> AiWordImportStep.TARGET_LANG
                AiWordImportStep.LEVEL -> AiWordImportStep.NATIVE_LANG
                AiWordImportStep.TOPICS -> AiWordImportStep.LEVEL
                AiWordImportStep.PREVIEW -> AiWordImportStep.TOPICS
            }
            copy(step = prev, error = null)
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
                interests = state.selectedTopics.toList()
            )
            submitPreferencesUseCase(preferences)
                .onSuccess { response ->
                    val allIndices = response.suggestedVocabulary.indices.toSet()
                    updateState {
                        copy(
                            isLoading = false,
                            step = AiWordImportStep.PREVIEW,
                            suggestedWords = response.suggestedVocabulary,
                            selectedWordIndices = allIndices,
                            error = null
                        )
                    }
                }
                .onFailure { error ->
                    updateState { copy(isLoading = false, error = error.message) }
                }
        }
    }

    fun importSelected() {
        val state = currentState
        val wordsToImport = state.selectedWordIndices
            .sorted()
            .mapNotNull { state.suggestedWords.getOrNull(it) }

        if (wordsToImport.isEmpty()) return

        viewModelScope.launch {
            updateState { copy(isLoading = true, error = null) }
            importSuggestedVocabularyUseCase(wordsToImport)
                .onSuccess { count ->
                    updateState { copy(isLoading = false) }
                    emitEffect(Event.ImportSuccess(count))
                }
                .onFailure { error ->
                    updateState { copy(isLoading = false, error = error.message) }
                }
        }
    }

    fun dismiss() {
        emitEffect(Event.Dismiss)
    }

    fun reset() {
        updateState { initialState() }
    }
}

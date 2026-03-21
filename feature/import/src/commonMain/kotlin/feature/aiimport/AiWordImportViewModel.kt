package feature.aiimport

import analytics.IAnalyticsTracker
import androidx.lifecycle.viewModelScope
import core.common.onFailure
import core.common.onSuccess
import domain.onboarding.model.OnboardingPreferences
import domain.onboarding.usecase.ImportSuggestedVocabularyUseCase
import domain.onboarding.usecase.SubmitPreferencesUseCase
import kotlinx.coroutines.launch
import core.base.BaseViewModel
import feature.aiimport.model.AiWordImportEffect
import feature.aiimport.model.AiWordImportStep
import feature.aiimport.model.AiWordImportUiState

class AiWordImportViewModel(
    private val submitPreferencesUseCase: SubmitPreferencesUseCase,
    private val importSuggestedVocabularyUseCase: ImportSuggestedVocabularyUseCase,
    private val analyticsTracker: IAnalyticsTracker,
) : BaseViewModel<AiWordImportUiState, AiWordImportEffect>() {

    override fun initialState() = AiWordImportUiState()

    init {
        analyticsTracker.logEvent("import_started")
    }

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
        val isAdding = !currentState.selectedTopics.contains(topic)
        updateState {
            val updated = if (selectedTopics.contains(topic)) {
                selectedTopics - topic
            } else {
                selectedTopics + topic
            }
            copy(selectedTopics = updated)
        }
        if (isAdding) {
            analyticsTracker.logEvent("import_topic_entered", mapOf("topic" to topic))
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
                    analyticsTracker.logEvent(
                        "import_preview_shown",
                        mapOf("word_count" to response.suggestedVocabulary.size.toString())
                    )
                }
                .onFailure { error ->
                    updateState { copy(isLoading = false, error = error.message) }
                    analyticsTracker.logEvent("import_failed", mapOf("reason" to (error.message ?: "unknown")))
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
                    analyticsTracker.logWordsImported(count = count, method = "ai")
                    analyticsTracker.logEvent("import_confirmed", mapOf("word_count" to count.toString()))
                    emitEffect(AiWordImportEffect.ImportSuccess(count))
                }
                .onFailure { error ->
                    updateState { copy(isLoading = false, error = error.message) }
                    analyticsTracker.logEvent("import_failed", mapOf("reason" to (error.message ?: "unknown")))
                }
        }
    }

    fun dismiss() {
        analyticsTracker.logEvent("import_cancelled")
        emitEffect(AiWordImportEffect.Dismiss)
    }

    fun reset() {
        updateState { initialState() }
    }
}

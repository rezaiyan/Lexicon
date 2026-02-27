package presentation.viewmodel

import analytics.IAnalyticsTracker
import domain.word.usecase.BatchUpdateLanguagesResult
import domain.word.usecase.BatchUpdateLanguagesUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import presentation.model.WordManagerEffect
import presentation.model.WordManagerScreenState
import utils.Language

class WordBatchEditHandler(
    private val batchUpdateLanguagesUseCase: BatchUpdateLanguagesUseCase,
    private val analyticsTracker: IAnalyticsTracker,
    private val state: MutableStateFlow<WordManagerScreenState>,
    private val events: SendChannel<WordManagerEffect>,
    private val scope: CoroutineScope
) {

    fun batchUpdateLanguages(
        selectedIds: List<Int>,
        sourceLanguage: Language,
        targetLanguage: Language
    ) {
        if (selectedIds.isEmpty()) {
            events.trySend(WordManagerEffect.Error("No words selected"))
            return
        }

        if (state.value.isBatchUpdatingLanguages) return

        scope.launch {
            batchUpdateLanguagesUseCase(
                wordIds = selectedIds,
                sourceLanguage = sourceLanguage.code,
                targetLanguage = targetLanguage.code
            ).collect { result ->
                when (result) {
                    is BatchUpdateLanguagesResult.Updating,
                    is BatchUpdateLanguagesResult.UpdatingBackend,
                    is BatchUpdateLanguagesResult.UpdatingLocal -> {
                        state.value = state.value.copy(
                            isBatchUpdatingLanguages = true,
                            errorMessage = null
                        )
                    }

                    is BatchUpdateLanguagesResult.Success -> {
                        state.value = state.value.copy(
                            isBatchUpdatingLanguages = false,
                            showBatchEditLanguages = false,
                            selectedWordIds = emptySet(),
                            errorMessage = null
                        )

                        events.send(WordManagerEffect.WordsLanguageUpdated(result.count))

                        analyticsTracker.logEvent(
                            "word_manager_languages_updated",
                            mapOf(
                                "count" to result.count.toString(),
                                "source_language" to sourceLanguage.code,
                                "target_language" to targetLanguage.code
                            )
                        )
                    }

                    is BatchUpdateLanguagesResult.Error -> {
                        state.value = state.value.copy(
                            isBatchUpdatingLanguages = false,
                            showBatchEditLanguages = false,
                            errorMessage = result.message
                        )

                        events.send(WordManagerEffect.Error(result.message))

                        analyticsTracker.logNonFatalError(
                            message = "Batch language update failed",
                            additionalInfo = mapOf(
                                "error" to result.message,
                                "count" to selectedIds.size.toString()
                            )
                        )
                    }
                }
            }
        }
    }
}

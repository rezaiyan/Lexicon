package presentation.viewmodel

import analytics.IAnalyticsTracker
import core.common.fold
import domain.word.model.Word
import domain.word.usecase.UpdateWordUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import presentation.model.WordManagerEffect
import presentation.model.WordManagerScreenState

class WordEditingHandler(
    private val updateWordUseCase: UpdateWordUseCase,
    private val analyticsTracker: IAnalyticsTracker,
    private val state: MutableStateFlow<WordManagerScreenState>,
    private val events: SendChannel<WordManagerEffect>,
    private val scope: CoroutineScope
) {
    
    fun startEditing(word: Word) {
        state.value = state.value.copy(detailWord = word)
        analyticsTracker.logEvent("word_manager_edit_started")
    }
    
    fun cancelEditing() {
        state.value = state.value.copy(detailWord = null)
    }
    
    fun updateWord(word: Word) {
        scope.launch {
            val result = updateWordUseCase(word)
            result.fold(
                onSuccess = { updatedWord ->
                    state.value = state.value.copy(detailWord = null)
                    events.send(WordManagerEffect.WordUpdated(updatedWord))
                    analyticsTracker.logEvent("word_manager_word_updated")
                },
                onFailure = { error ->
                    val errorMsg = error.message ?: ""
                    events.send(WordManagerEffect.Error(errorMsg))
                    analyticsTracker.logNonFatalError(
                        message = "Word update failed",
                        additionalInfo = mapOf("error" to (errorMsg.ifEmpty { "unknown" }))
                    )
                }
            )
        }
    }
}




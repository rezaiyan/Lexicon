package feature.words

import analytics.IAnalyticsTracker
import core.common.fold
import domain.word.model.Word
import domain.word.usecase.UpdateWordUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import core.base.BaseViewModel
import feature.words.model.WordManagerEffect
import feature.words.model.WordManagerScreenState

class WordEditingHandler(
    private val updateWordUseCase: UpdateWordUseCase,
    private val analyticsTracker: IAnalyticsTracker,
    private val stateAccess: BaseViewModel.StateAccess<WordManagerScreenState>,
    private val events: SendChannel<WordManagerEffect>,
    private val scope: CoroutineScope
) {

    fun startEditing(word: Word) {
        stateAccess.update { copy(detailWord = word) }
        analyticsTracker.logEvent("word_manager_edit_started")
    }

    fun cancelEditing() {
        stateAccess.update { copy(detailWord = null) }
    }

    fun updateWord(word: Word) {
        scope.launch {
            val result = updateWordUseCase(word)
            result.fold(
                onSuccess = { updatedWord ->
                    stateAccess.update { copy(detailWord = null) }
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

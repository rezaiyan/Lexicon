package feature.words

import analytics.IAnalyticsTracker
import core.common.fold
import domain.word.model.Word
import domain.word.usecase.UpdateWordUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import feature.words.model.WordManagerEffect

class WordEditingHandler(
    private val updateWordUseCase: UpdateWordUseCase,
    private val analyticsTracker: IAnalyticsTracker,
    private val events: SendChannel<WordManagerEffect>,
    private val scope: CoroutineScope
) {

    fun updateWord(word: Word) {
        scope.launch {
            val result = updateWordUseCase(word)
            result.fold(
                onSuccess = { updatedWord ->
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

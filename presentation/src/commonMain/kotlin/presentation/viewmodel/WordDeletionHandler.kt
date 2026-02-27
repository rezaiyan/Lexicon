package presentation.viewmodel

import analytics.IAnalyticsTracker
import domain.word.usecase.DeleteWordsResult
import domain.word.usecase.DeleteWordsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import presentation.model.WordManagerEffect

class WordDeletionHandler(
    private val deleteWordsUseCase: DeleteWordsUseCase,
    private val analyticsTracker: IAnalyticsTracker,
    private val state: MutableStateFlow<presentation.model.WordManagerScreenState>,
    private val events: SendChannel<WordManagerEffect>,
    private val scope: CoroutineScope
) {
    
    fun deleteSelectedWords(selectedIds: List<Int>) {
        if (selectedIds.isEmpty()) {
            events.trySend(WordManagerEffect.Error("No words selected"))
            return
        }
        
        if (state.value.isDeletingWords) return
        
        scope.launch {
            deleteWordsUseCase(selectedIds).collect { result ->
                when (result) {
                    is DeleteWordsResult.Deleting,
                    is DeleteWordsResult.DeletingBackend,
                    is DeleteWordsResult.DeletingLocal -> {
                        state.value = state.value.copy(
                            isDeletingWords = true,
                            errorMessage = null
                        )
                    }
                    
                    is DeleteWordsResult.Success -> {
                        state.value = state.value.copy(
                            isDeletingWords = false,
                            isSelectionMode = false,
                            selectedWordIds = emptySet(),
                            showDeleteConfirmation = false,
                            errorMessage = null
                        )
                        
                        events.send(WordManagerEffect.WordDeleted(result.count))
                        
                        analyticsTracker.logEvent(
                            "word_manager_words_deleted",
                            mapOf(
                                "count" to result.count.toString(),
                                "batch" to "true",
                                "backend_and_local" to "true"
                            )
                        )
                    }
                    
                    is DeleteWordsResult.Error -> {
                        state.value = state.value.copy(
                            isDeletingWords = false,
                            showDeleteConfirmation = false,
                            errorMessage = result.message
                        )
                        
                        events.send(WordManagerEffect.Error(result.message))
                        
                        analyticsTracker.logNonFatalError(
                            message = "Batch delete failed",
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


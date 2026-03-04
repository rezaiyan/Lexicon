@file:OptIn(kotlin.time.ExperimentalTime::class)

package presentation.viewmodel

import analytics.IAnalyticsTracker
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import domain.word.model.Word
import domain.word.usecase.DeleteWordUseCase
import domain.word.usecase.GetDueWordsUseCase
import domain.word.usecase.GetWordsByStageUseCase
import domain.word.usecase.UpdateWordUseCase
import core.common.fold
import events.VocabularyEffect
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import presentation.model.ReviewMode
import presentation.model.UiMessage
import presentation.model.UiState

class VocabularyViewModel(
    private val getDueWordsUseCase: GetDueWordsUseCase,
    private val getWordsByStageUseCase: GetWordsByStageUseCase,
    private val updateWordUseCase: UpdateWordUseCase,
    private val deleteWordUseCase: DeleteWordUseCase,
    private val analyticsTracker: IAnalyticsTracker,
) : ViewModel() {

    private val _events = Channel<VocabularyEffect>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val _uiMessages = Channel<UiMessage>(Channel.BUFFERED)
    val uiMessages = _uiMessages.receiveAsFlow()

    private val _wordListState = MutableStateFlow<UiState<List<Word>>>(UiState.Loading)

    private var currentReviewMode: ReviewMode = ReviewMode.DuoCards

    fun loadWords(reviewMode: ReviewMode = ReviewMode.DuoCards) {
        currentReviewMode = reviewMode
        _wordListState.value = UiState.Loading

        viewModelScope.launch {
            when (reviewMode) {
                is ReviewMode.DuoCards -> getDueWordsUseCase()
                is ReviewMode.ByStage -> getWordsByStageUseCase(reviewMode.stage)
            }
                .catch { e -> _wordListState.value = UiState.Error(e.message ?: "Unknown error") }
                .collect { words -> _wordListState.value = UiState.Loaded(words) }
        }
    }

    fun updateWord(word: Word) {
        viewModelScope.launch {
            val result = updateWordUseCase(word)
            result.fold(
                onSuccess = {
                    loadWords(currentReviewMode)
                    analyticsTracker.logEvent("word_updated_in_review")
                },
                onFailure = { error ->
                    analyticsTracker.logNonFatalError(
                        message = "Word update failed in review",
                        additionalInfo = mapOf("error" to (error.message ?: "unknown"))
                    )
                }
            )
        }
    }

    fun deleteWord(wordId: Int, onDeleted: () -> Unit) {
        viewModelScope.launch {
            val result = deleteWordUseCase(wordId)
            result.fold(
                onSuccess = {
                    _uiMessages.send(UiMessage.WordDeleted)
                    loadWords(currentReviewMode)
                    onDeleted()
                    analyticsTracker.logEvent("word_deleted_in_review")
                },
                onFailure = { error ->
                    analyticsTracker.logNonFatalError(
                        message = "Word deletion failed in review",
                        additionalInfo = mapOf("error" to (error.message ?: "unknown"))
                    )
                }
            )
        }
    }
}

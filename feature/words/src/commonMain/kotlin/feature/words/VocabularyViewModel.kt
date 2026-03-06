@file:OptIn(kotlin.time.ExperimentalTime::class)

package feature.words

import analytics.IAnalyticsTracker
import androidx.lifecycle.viewModelScope
import domain.word.model.Word
import domain.word.usecase.DeleteWordUseCase
import domain.word.usecase.GetDueWordsUseCase
import domain.word.usecase.GetWordsByStageUseCase
import domain.word.usecase.UpdateWordUseCase
import core.common.fold
import events.VocabularyEffect
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import core.base.BaseViewModel
import feature.words.model.ReviewMode
import core.common.UiState

class VocabularyViewModel(
    private val getDueWordsUseCase: GetDueWordsUseCase,
    private val getWordsByStageUseCase: GetWordsByStageUseCase,
    private val updateWordUseCase: UpdateWordUseCase,
    private val deleteWordUseCase: DeleteWordUseCase,
    private val analyticsTracker: IAnalyticsTracker,
) : BaseViewModel<UiState<List<Word>>, VocabularyEffect>() {

    override fun initialState(): UiState<List<Word>> = UiState.Loading

    private var currentReviewMode: ReviewMode = ReviewMode.DuoCards

    fun loadWords(reviewMode: ReviewMode = ReviewMode.DuoCards) {
        currentReviewMode = reviewMode
        updateState { UiState.Loading }

        viewModelScope.launch {
            when (reviewMode) {
                is ReviewMode.DuoCards -> getDueWordsUseCase()
                is ReviewMode.ByStage -> getWordsByStageUseCase(reviewMode.stage)
            }
                .catch { e -> updateState { UiState.Error(e.message ?: "Unknown error") } }
                .collect { words -> updateState { UiState.Loaded(words) } }
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

    fun deleteWord(wordId: Int) {
        viewModelScope.launch {
            val result = deleteWordUseCase(wordId)
            result.fold(
                onSuccess = {
                    emitEffect(VocabularyEffect.WordDeleted)
                    loadWords(currentReviewMode)
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

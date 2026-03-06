package feature.study.model

import domain.word.model.ProgressEvaluation
import domain.word.model.ProgressStats
import domain.word.model.Word
import core.common.UiState

data class ProgressScreenState(
    val progressStats: ProgressStats,
    val progressEvaluation: ProgressEvaluation,
    val messageState: MessageState? = null
)

sealed class MessageState {
    data class Error(val message: String) : MessageState()
}

data class ReviewScreenState(
    val wordListState: UiState<List<Word>> = UiState.Loading,
    val reviewType: ReviewType = ReviewType.REVIEW
)

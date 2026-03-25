package feature.study.model

import domain.word.model.ProgressEvaluation
import domain.word.model.ProgressStats
import core.common.UiState

data class ProgressScreenState(
    val progressStats: ProgressStats,
    val progressEvaluation: ProgressEvaluation,
    val messageState: MessageState? = null
)

sealed class MessageState {
    data class Error(val message: String) : MessageState()
}

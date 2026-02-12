package presentation.model

import domain.word.model.LearningStage

sealed class ReviewMode {
    data object DuoCards : ReviewMode()
    data class ByStage(val stage: LearningStage) : ReviewMode()
}


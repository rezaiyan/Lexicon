package domain.word.model

/** Describes where the review queue should be loaded from. */
sealed class ReviewSource {
    data object DueCards : ReviewSource()
    data class ByStage(val stage: LearningStage) : ReviewSource()
    data class ByTag(val tagId: Long) : ReviewSource()
    data class ByStageAndTag(val stage: LearningStage, val tagId: Long) : ReviewSource()
}

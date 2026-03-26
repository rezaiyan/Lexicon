package domain.word.model

/** Describes where the review queue should be loaded from. */
sealed class ReviewSource {
    /** Label used when recording a study session type. */
    abstract val sessionTypeLabel: String

    data object DueCards : ReviewSource() {
        override val sessionTypeLabel: String = "REVIEW"
    }

    data class ByStage(val stage: LearningStage) : ReviewSource() {
        override val sessionTypeLabel: String = "BROWSE"
    }

    data class ByTag(val tagId: Long) : ReviewSource() {
        override val sessionTypeLabel: String = "REVIEW"
    }

    data class ByStageAndTag(val stage: LearningStage, val tagId: Long) : ReviewSource() {
        override val sessionTypeLabel: String = "BROWSE"
    }
}

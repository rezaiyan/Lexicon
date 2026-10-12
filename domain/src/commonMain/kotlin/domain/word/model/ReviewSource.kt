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

    /** A hand-picked set of words, e.g. from an Insights coach card. Not capped by the daily goal. */
    data class ByWords(val wordIds: List<Long>) : ReviewSource() {
        override val sessionTypeLabel: String = "REVIEW"
    }

    /**
     * A comeback rescue from a push: the words a nightly plan picked for this learner, in session
     * order (easy wins first and last). Not filtered by learning focus or capped by the daily goal.
     */
    data class ComebackRescue(
        val rescueId: Long,
        val wordIds: List<Long>,
        val outroWin: String? = null,
        val outroSoft: String? = null,
    ) : ReviewSource() {
        override val sessionTypeLabel: String = "REVIEW"
    }

    data class ByStageAndTag(val stage: LearningStage, val tagId: Long) : ReviewSource() {
        override val sessionTypeLabel: String = "BROWSE"
    }
}

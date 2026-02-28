package domain.word.usecase

import domain.word.model.ProgressEvaluation
import domain.word.model.ProgressStats
import domain.word.model.ProgressTier

/**
 * Evaluates [ProgressStats] into a [ProgressEvaluation] with a weighted mastery
 * score and a fine-grained [ProgressTier] suitable for motivational messaging.
 *
 * **Weighted scoring:** each SRS level contributes proportionally
 * (Level N = N / 6 of full mastery). Level 0 (fresh) words are excluded
 * from the numerator so newly added words don't inflate the score.
 */
class EvaluateProgressUseCase {

    operator fun invoke(stats: ProgressStats): ProgressEvaluation {
        val progressFraction = if (stats.totalWords > 0) {
            val weightedScore = (
                    stats.level1Count * 1 +
                            stats.level2Count * 2 +
                            stats.level3Count * 3 +
                            stats.level4Count * 4 +
                            stats.level5Count * 5 +
                            stats.level6Count * 6
                    ).toFloat()
            (weightedScore / (stats.totalWords * MAX_LEVEL_WEIGHT)).coerceIn(0f, 1f)
        } else {
            0f
        }

        val progressPercent = (progressFraction * 100).toInt()

        val tier = when {
            stats.totalWords == 0 -> ProgressTier.EMPTY
            progressPercent >= 100 -> ProgressTier.MASTERED
            progressPercent >= 90 -> ProgressTier.ALMOST_MASTER
            progressPercent >= 75 -> ProgressTier.STRONG
            progressPercent >= 50 -> ProgressTier.HALFWAY
            progressPercent >= 25 -> ProgressTier.PROGRESSING
            progressPercent >= 10 -> ProgressTier.BUILDING
            else -> ProgressTier.GETTING_STARTED
        }

        return ProgressEvaluation(
            progressFraction = progressFraction,
            progressPercent = progressPercent,
            tier = tier
        )
    }

    companion object {
        private const val MAX_LEVEL_WEIGHT = 6f
    }
}

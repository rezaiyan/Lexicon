package domain.word.usecase

import core.common.Try
import core.common.UseCase
import core.common.flatMap
import domain.settings.model.ReviewSettings
import domain.word.model.ReviewWordResult
import domain.word.model.Word
import domain.word.repository.IReviewSyncRepository
import domain.word.repository.IWordRepository
import kotlin.math.max
import kotlin.math.min
import kotlin.time.Clock

/**
 * 7-Bucket Spaced Repetition System
 *
 * BUCKETS (Levels 0–6):
 * - Level 0: New           (1 minute)
 * - Level 1: Learning      (10 minutes)
 * - Level 2: Familiarizing (1 day)
 * - Level 3: Consolidating (3 days)
 * - Level 4: Young         (7 days)
 * - Level 5: Mature        (14 days)
 * - Level 6: Mastered      (30+ days, growing)
 *
 * RESPONSES (2-button):
 * - quality = 0: FORGOT  — drop [ReviewSettings.forgotPenalty] levels
 * - quality = 1: REMEMBERED — advance after [ReviewSettings.successesToAdvance] successes
 *
 * Returns [ReviewWordResult] so callers can detect mastery without duplicating
 * the SRS algorithm.
 */
class ReviewWordUseCase(
    private val wordRepository: IWordRepository,
    private val reviewSyncRepository: IReviewSyncRepository,
) : UseCase<ReviewWordUseCase.Params, ReviewWordResult> {

    companion object {
        private val LEVEL_INTERVALS = mapOf(
            0 to 1,
            1 to 10,
            2 to 1,
            3 to 3,
            4 to 7,
            5 to 14,
            6 to 30,
        )
        private val SETTINGS = ReviewSettings.BALANCED
    }

    data class Params(val word: Word, val quality: Int)

    override suspend fun invoke(params: Params): Try<ReviewWordResult> =
        invoke(params.word, params.quality)

    suspend operator fun invoke(word: Word, quality: Int): Try<ReviewWordResult> {
        val previousLevel = word.level
        val computed = applySpacedRepetition(word, quality)
        return wordRepository.updateWordLocal(computed)
            .flatMap { reviewSyncRepository.enqueue(computed.id) }
            .flatMap { Try.success(ReviewWordResult(computed, previousLevel)) }
    }

    // ---------------------------------------------------------------------------
    // Internal SRS computation
    // ---------------------------------------------------------------------------

    private fun applySpacedRepetition(word: Word, quality: Int): Word {
        val now = Clock.System.now().toEpochMilliseconds()
        val minuteMs = 60 * 1_000L
        val dayMs = 24 * 60 * minuteMs

        val newLevel: Int
        val newEaseFactor: Float
        val newInterval: Int
        val newRepetitions: Int

        if (quality == 0) {
            // Forgot — drop back and restart
            newLevel = max(0, word.level - SETTINGS.forgotPenalty)
            newRepetitions = 0
            newInterval = LEVEL_INTERVALS[newLevel] ?: 1
            newEaseFactor = max(1.3f, word.easeFactor - 0.2f)
        } else {
            // Remembered (any quality ≥ 1)
            val repetitions = word.repetitions + 1
            if (repetitions >= SETTINGS.successesToAdvance && word.level < 6) {
                // Advance to next bucket
                newLevel = min(6, word.level + 1)
                newRepetitions = 0
                newInterval = LEVEL_INTERVALS[newLevel] ?: 1
                newEaseFactor = min(2.5f, word.easeFactor + 0.1f)
            } else {
                // Stay at current level
                newLevel = word.level
                newRepetitions = repetitions
                newEaseFactor = word.easeFactor
                newInterval = if (word.level == 6 && repetitions > 0) {
                    min((word.interval * word.easeFactor).toInt(), 365)
                } else {
                    LEVEL_INTERVALS[word.level] ?: 1
                }
            }
        }

        val nextReview = if (newLevel <= 1) now + newInterval * minuteMs
                         else              now + newInterval * dayMs

        return word.copy(
            level = newLevel,
            easeFactor = newEaseFactor,
            interval = newInterval,
            repetitions = newRepetitions,
            lastReviewDate = now,
            nextReviewDate = nextReview,
        )
    }
}

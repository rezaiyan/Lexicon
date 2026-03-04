@file:OptIn(ExperimentalTime::class)

package domain.word.usecase

import core.common.Try
import core.common.UseCase
import core.common.getOrThrow
import domain.settings.usecase.GetReviewSettingsUseCase
import domain.word.model.Word
import domain.word.repository.IWordRepository
import kotlin.math.max
import kotlin.math.min
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * 7-Bucket Spaced Repetition System
 *
 * BUCKETS (Levels 0-6):
 * - Level 0: New (review immediately)
 * - Level 1: Learning (10 minutes)
 * - Level 2: Familiarizing (1 day)
 * - Level 3: Consolidating (3 days)
 * - Level 4: Young (7 days)
 * - Level 5: Mature (14 days)
 * - Level 6: Mastered (30+ days, increasing)
 *
 * RESPONSES (2-button system):
 * - quality = 0: FORGOT (move down or restart)
 * - quality = 1: REMEMBERED (move up with appropriate interval)
 *
 * LOGIC (Configurable via user settings):
 * - FORGOT: Drop by N levels (configurable: 1-3, default 2)
 * - REMEMBERED: Advance after N successes (configurable: 1-3, default 1)
 *   - Level 6 (Mastered): interval increases exponentially
 */
class ReviewWordUseCase(
    private val wordRepository: IWordRepository,
    private val getReviewSettingsUseCase: GetReviewSettingsUseCase
) : UseCase<ReviewWordUseCase.Params, Unit> {
    companion object {
        // Interval definitions (in minutes for level 0-1, days for level 2-6)
        private val LEVEL_INTERVALS = mapOf(
            0 to 1,      // 1 minute (immediate review)
            1 to 10,     // 10 minutes
            2 to 1,      // 1 day (first day review)
            3 to 3,      // 3 days
            4 to 7,      // 1 week
            5 to 14,     // 2 weeks
            6 to 30      // 1 month (base, then grows)
        )
    }

    data class Params(val word: Word, val quality: Int)

    override suspend operator fun invoke(params: Params) =
        invoke(params.word, params.quality)

    suspend operator fun invoke(word: Word, quality: Int): Try<Unit> = Try {
        // Get current review settings
        val settings = getReviewSettingsUseCase(Unit).getOrThrow()

        val now = Clock.System.now().toEpochMilliseconds()
        val minuteInMillis = 60 * 1000L
        val dayInMillis = 24 * 60 * minuteInMillis

        var newLevel = word.level
        var newEaseFactor = word.easeFactor
        var newInterval: Int
        var newRepetitions = word.repetitions

        when (quality) {
            0 -> { // FORGOT - drop back and restart progress
                // Drop by configured penalty levels (minimum 0)
                newLevel = max(0, word.level - settings.forgotPenalty)
                newRepetitions = 0

                // Reset to initial interval for new level
                newInterval = LEVEL_INTERVALS[newLevel] ?: 1

                // Reduce ease factor (harder next time)
                newEaseFactor = max(1.3f, word.easeFactor - 0.2f)
            }

            1 -> { // REMEMBERED - maintain or advance
                newRepetitions = word.repetitions + 1

                // Check if we should advance to next level (using configured threshold)
                if (newRepetitions >= settings.successesToAdvance && word.level < 6) {
                    // Advance to next bucket
                    newLevel = min(6, word.level + 1)
                    newRepetitions = 0 // Reset counter for new level
                    newInterval = LEVEL_INTERVALS[newLevel] ?: 1

                    // Improve ease factor (easier next time)
                    newEaseFactor = min(2.5f, word.easeFactor + 0.1f)
                } else {
                    // Stay at current level, repeat interval
                    newLevel = word.level
                    newInterval = LEVEL_INTERVALS[word.level] ?: 1

                    // For mastered level (6), increase interval exponentially
                    if (word.level == 6 && newRepetitions > 0) {
                        // Each success at mastered level increases interval
                        newInterval = (word.interval * newEaseFactor).toInt()
                        newInterval = min(newInterval, 365) // Cap at 1 year
                    }
                }
            }

            else -> {
                // Invalid quality - treat as FORGOT (use configured penalty)
                newLevel = max(0, word.level - settings.forgotPenalty)
                newRepetitions = 0
                newInterval = LEVEL_INTERVALS[newLevel] ?: 1
                newEaseFactor = max(1.3f, word.easeFactor - 0.2f)
            }
        }

        // Calculate next review date
        val nextReview = when (newLevel) {
            0, 1 -> now + (newInterval * minuteInMillis) // Minutes for early levels
            else -> now + (newInterval * dayInMillis)      // Days for advanced levels
        }

        val updatedWord = word.copy(
            level = newLevel,
            easeFactor = newEaseFactor,
            interval = newInterval,
            repetitions = newRepetitions,
            lastReviewDate = now,
            nextReviewDate = nextReview
        )

        wordRepository.updateWord(updatedWord)
    }
}

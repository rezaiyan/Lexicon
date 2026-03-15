package domain.widget.usecase

import core.common.NoParamUseCase
import core.common.Try
import core.common.flatMap
import core.common.getOrDefault
import core.common.map
import domain.streak.repository.IStreakRepository
import domain.widget.model.DailyWidgetData
import domain.word.repository.IWordRepository
import kotlin.random.Random

/**
 * Use case that provides data for the daily word widget.
 *
 * Selects a word deterministically per day (using the day-of-epoch as seed)
 * so the widget shows the same word throughout the day, and pairs it with
 * the user's current streak count and due-card count.
 */
class GetDailyWidgetDataUseCase(
    private val wordRepository: IWordRepository,
    private val streakRepository: IStreakRepository
) : NoParamUseCase<DailyWidgetData> {

    override suspend operator fun invoke(params: Unit): Try<DailyWidgetData> {
        return wordRepository.getAllWordsAsync().flatMap { words ->
            if (words.isEmpty()) {
                return@flatMap Try.failure(NoWordsAvailableException())
            }

            // Pick a word deterministically based on the day so it stays consistent
            val daysSinceEpoch = currentDaysSinceEpoch()
            val random = Random(daysSinceEpoch.toLong())
            val selectedWord = words[random.nextInt(words.size)]

            val streakCount = streakRepository.getStreak()
                .map { it.currentStreak }
                .getOrDefault(0)

            val dueCount = wordRepository.getDueCount().getOrDefault(0)

            Try.success(
                DailyWidgetData(
                    word = selectedWord.originalWord,
                    translation = selectedWord.translation,
                    streakCount = streakCount,
                    dueCardCount = dueCount
                )
            )
        }
    }

    /**
     * Returns the number of days since Unix epoch.
     * Uses pure Kotlin time APIs to avoid platform dependencies.
     */
    private fun currentDaysSinceEpoch(): Int {
        val nowMillis = kotlin.time.Clock.System.now().toEpochMilliseconds()
        return (nowMillis / MILLIS_PER_DAY).toInt()
    }

    companion object {
        private const val MILLIS_PER_DAY = 86_400_000L
    }
}

class NoWordsAvailableException : Exception("No words available for widget")

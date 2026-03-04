package domain.streak.usecase

import core.common.Try
import core.common.UseCase
import domain.streak.model.StreakData
import domain.streak.repository.IStreakRepository

/**
 * Use case to record user activity for streak tracking.
 * Records that the user performed a learning activity (e.g., reviewed words).
 */
class RecordStreakActivityUseCase(
    private val streakRepository: IStreakRepository
) : UseCase<Int, StreakData> {
    override suspend operator fun invoke(count: Int): Try<StreakData> {
        return streakRepository.recordActivity(count)
    }
}

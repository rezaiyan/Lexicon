package domain.streak.usecase

import domain.common.Try
import domain.streak.model.StreakData
import domain.streak.repository.IStreakRepository

/**
 * Use case to record user activity for streak tracking.
 * Records that the user performed a learning activity (e.g., reviewed words).
 */
class RecordStreakActivityUseCase(
    private val streakRepository: IStreakRepository
) {
    suspend operator fun invoke(): Try<StreakData> {
        return streakRepository.recordActivity()
    }
}

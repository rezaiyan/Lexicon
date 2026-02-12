package domain.streak.usecase

import domain.streak.repository.IStreakRepository

class GetStreakUseCase(
    private val streakRepository: IStreakRepository
) {
    suspend fun getUserStreaks() = streakRepository.getStreak()
}
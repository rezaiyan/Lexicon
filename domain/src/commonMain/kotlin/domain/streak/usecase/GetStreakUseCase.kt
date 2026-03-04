package domain.streak.usecase

import core.common.NoParamUseCase
import core.common.Try
import domain.streak.model.StreakData
import domain.streak.repository.IStreakRepository

class GetStreakUseCase(
    private val streakRepository: IStreakRepository
) : NoParamUseCase<StreakData> {
    override suspend operator fun invoke(params: Unit): Try<StreakData> {
        return streakRepository.getStreak()
    }
}

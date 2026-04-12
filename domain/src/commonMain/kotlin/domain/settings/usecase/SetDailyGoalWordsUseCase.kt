package domain.settings.usecase

import core.common.Try
import core.common.UseCase
import domain.settings.repository.ISettingsRepository

class SetDailyGoalWordsUseCase(
    private val settingsRepository: ISettingsRepository
) : UseCase<Int, Unit> {
    override suspend operator fun invoke(params: Int): Try<Unit> =
        settingsRepository.setDailyGoalWords(params)
}

package domain.settings.usecase

import core.common.NoParamUseCase
import core.common.Try
import domain.settings.repository.ISettingsRepository

class GetDailyGoalWordsUseCase(
    private val settingsRepository: ISettingsRepository
) : NoParamUseCase<Int> {

    override suspend operator fun invoke(params: Unit) = invoke()

    suspend operator fun invoke(): Try<Int> = settingsRepository.getDailyGoalWords()
}

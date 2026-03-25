package domain.settings.usecase

import core.common.Try
import core.common.UseCase
import domain.settings.repository.ISettingsRepository

class SetSkipTagSelectorUseCase(
    private val settingsRepository: ISettingsRepository
) : UseCase<Boolean, Unit> {
    override suspend operator fun invoke(params: Boolean): Try<Unit> =
        settingsRepository.setSkipTagSelector(params)
}

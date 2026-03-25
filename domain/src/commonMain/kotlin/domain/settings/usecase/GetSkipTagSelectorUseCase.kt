package domain.settings.usecase

import core.common.NoParamFlowUseCase
import domain.settings.repository.ISettingsRepository
import kotlinx.coroutines.flow.Flow

class GetSkipTagSelectorUseCase(
    private val settingsRepository: ISettingsRepository
) : NoParamFlowUseCase<Boolean> {
    override operator fun invoke(params: Unit): Flow<Boolean> =
        settingsRepository.getSkipTagSelector()
}

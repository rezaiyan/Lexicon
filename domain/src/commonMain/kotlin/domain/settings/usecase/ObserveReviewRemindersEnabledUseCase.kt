package domain.settings.usecase

import core.common.NoParamFlowUseCase
import domain.settings.repository.ISettingsRepository

/** Exposes the review-reminders enabled flag as a stream — hides the full settings repository. */
class ObserveReviewRemindersEnabledUseCase(
    private val settingsRepository: ISettingsRepository,
) : NoParamFlowUseCase<Boolean> {
    override fun invoke(params: Unit) = settingsRepository.getReviewRemindersEnabled()
}

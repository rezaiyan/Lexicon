package domain.settings.usecase

import core.common.NoParamUseCase
import core.common.Try
import domain.settings.model.ReviewSettings

/**
 * Use case to get current review settings
 *
 * Returns the default BALANCED mode settings.
 * Settings are no longer user-configurable - simplified to client-side only.
 */
class GetReviewSettingsUseCase : NoParamUseCase<ReviewSettings> {
    override suspend operator fun invoke(params: Unit): Try<ReviewSettings> {
        return Try.success(ReviewSettings.BALANCED)
    }
}

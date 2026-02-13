package domain.settings.usecase

import domain.settings.model.ReviewSettings

/**
 * Use case to get current review settings
 *
 * Returns the default BALANCED mode settings.
 * Settings are no longer user-configurable - simplified to client-side only.
 */
class GetReviewSettingsUseCase {
    operator fun invoke(): ReviewSettings {
        return ReviewSettings.BALANCED
    }
}


package domain.settings.usecase

import domain.settings.model.ReviewSettings
import domain.settings.repository.ISettingsRepository

/**
 * Use case to update review settings
 * 
 * Encapsulates the logic for updating review configuration
 */
class UpdateReviewSettingsUseCase(
    private val settingsRepository: ISettingsRepository
) {
    suspend operator fun invoke(settings: ReviewSettings) {
        settingsRepository.setSuccessesToAdvance(settings.successesToAdvance)
        settingsRepository.setForgotPenalty(settings.forgotPenalty)
    }
}


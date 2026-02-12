package domain.settings.usecase

import domain.settings.model.ReviewSettings
import domain.settings.repository.ISettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.catch

/**
 * Use case to get current review settings
 * 
 * Single source of truth for review/learning configuration
 */
class GetReviewSettingsUseCase(
    private val settingsRepository: ISettingsRepository
) {
    suspend operator fun invoke(): ReviewSettings {
        return try {
            val successesToAdvance = settingsRepository.getSuccessesToAdvance()
                .catch { emit(ReviewSettings.BALANCED.successesToAdvance) }
                .first()
            
            val forgotPenalty = settingsRepository.getForgotPenalty()
                .catch { emit(ReviewSettings.BALANCED.forgotPenalty) }
                .first()

            ReviewSettings(
                successesToAdvance = successesToAdvance,
                forgotPenalty = forgotPenalty
            )
        } catch (e: IllegalArgumentException) {
            ReviewSettings.BALANCED
        } catch (e: Exception) {
            ReviewSettings.BALANCED
        }
    }
}


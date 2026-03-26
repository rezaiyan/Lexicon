package domain.startup.usecase

import core.common.Try
import core.common.UseCase
import core.common.getOrElse
import domain.onboarding.repository.IOnboardingRepository
import domain.startup.model.AppStartupDestination
import domain.word.repository.IWordRepository

/**
 * Determines where the app should navigate after a user completes authentication,
 * when we need to check whether they are a returning user (has existing word data)
 * or a new user who should go through onboarding.
 *
 * Input: [Unit]
 * Output: [AppStartupDestination] — either [AppStartupDestination.Ready] or
 *   [AppStartupDestination.Onboarding].
 */
class DeterminePostAuthDestinationUseCase(
    private val onboardingRepository: IOnboardingRepository,
    private val wordRepository: IWordRepository,
) : UseCase<Unit, AppStartupDestination> {

    override suspend fun invoke(params: Unit): Try<AppStartupDestination> {
        val wordCount = wordRepository.getTotalCount().getOrElse { 0 }
        val destination = if (wordCount > 0) {
            onboardingRepository.markOnboardingCompleted()
            AppStartupDestination.Ready
        } else {
            AppStartupDestination.Onboarding
        }
        return Try.success(destination)
    }
}

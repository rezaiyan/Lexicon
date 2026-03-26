package domain.startup.usecase

import core.common.UseCase
import core.common.getOrDefault
import core.common.getOrElse
import domain.onboarding.repository.IOnboardingRepository
import domain.startup.model.AppStartupDestination
import domain.word.repository.IWordRepository

/**
 * Determines where the app should navigate after session verification.
 *
 * Input: [Boolean] — whether the user is currently authenticated.
 * Output: [AppStartupDestination] — the navigation target.
 */
class DetermineAppStartupStateUseCase(
    private val onboardingRepository: IOnboardingRepository,
    private val wordRepository: IWordRepository,
) : UseCase<Boolean, AppStartupDestination> {

    override suspend fun invoke(params: Boolean): core.common.Try<AppStartupDestination> {
        val isAuthenticated = params
        val onboardingCompleted = onboardingRepository.hasCompletedOnboarding().getOrDefault(false)

        val destination = when {
            onboardingCompleted && isAuthenticated -> AppStartupDestination.Ready

            onboardingCompleted -> AppStartupDestination.RequiresAuth(needsOnboardingCheck = false)

            isAuthenticated -> {
                // Authenticated but no onboarding flag — returning user after reinstall.
                // Skip onboarding and mark it completed.
                onboardingRepository.markOnboardingCompleted()
                AppStartupDestination.Ready
            }

            else -> {
                // Not authenticated, no onboarding flag — new user.
                // After login we need to check whether they have existing data.
                AppStartupDestination.RequiresAuth(needsOnboardingCheck = true)
            }
        }

        return core.common.Try.success(destination)
    }
}

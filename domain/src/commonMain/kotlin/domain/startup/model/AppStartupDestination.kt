package domain.startup.model

/**
 * Domain representation of where the app should navigate after startup/auth verification.
 * ViewModels map this to platform-specific UI state (e.g. AppUiState in presentation layer).
 */
sealed interface AppStartupDestination {
    /** User is authenticated and has completed onboarding — show main content. */
    data object Ready : AppStartupDestination

    /** User needs to complete onboarding flow. */
    data object Onboarding : AppStartupDestination

    /**
     * User needs to go through authentication.
     *
     * @param needsOnboardingCheck when true, after login the app should check whether to
     *   show onboarding (new user) or go straight to Ready (returning user with data).
     */
    data class RequiresAuth(val needsOnboardingCheck: Boolean = false) : AppStartupDestination
}

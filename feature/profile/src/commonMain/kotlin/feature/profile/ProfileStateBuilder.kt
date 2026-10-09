package feature.profile

import domain.credits.model.CreditBalance
import domain.auth.model.AuthUser
import domain.auth.model.FeatureAccessResponse
import domain.auth.model.PremiumSource
import domain.auth.model.UserFeatureAccess
import domain.common.util.EpochDateFormatter
import domain.streak.model.StreakData
import feature.profile.model.ProfileStatsUiModel
import feature.profile.model.ProfileSubscriptionStatus
import feature.profile.model.ProfileUiData
import feature.profile.model.ProfileUserUiModel
import core.common.UiState

internal object ProfileStateBuilder {

    fun createUiState(
        user: AuthUser?,
        streak: UiState<StreakData>,
        featureAccessState: UiState<FeatureAccessResponse?>,
        profileStats: ProfileStatsUiModel?,
        credits: CreditBalance? = null,
    ): UiState<ProfileUiData> {
        return when {
            user == null -> createUnauthenticatedState()
            featureAccessState is UiState.Loading -> UiState.Loading
            streak is UiState.Error -> UiState.Error(streak.message)
            else -> {
                val featureAccess = (featureAccessState as? UiState.Loaded)?.value
                createLoadedState(user, streak, featureAccess, profileStats, credits)
            }
        }
    }

    private fun createUnauthenticatedState(): UiState.Loaded<ProfileUiData> {
        return UiState.Loaded(
            ProfileUiData(
                userInfo = null,
                streak = null,
                featureAccess = null,
                isSubscriptionsEnabled = false,
                shouldShowSubscriptionUI = false
            )
        )
    }

    private fun createLoadedState(
        user: AuthUser,
        streak: UiState<StreakData>,
        featureAccess: FeatureAccessResponse?,
        profileStats: ProfileStatsUiModel?,
        credits: CreditBalance?,
    ): UiState.Loaded<ProfileUiData> {
        val streakData = when (streak) {
            is UiState.Loaded -> streak.value
            else -> null
        }

        val hasPremiumAccess = featureAccess?.userAccess?.hasPremiumAccess == true

        return UiState.Loaded(
            ProfileUiData(
                userInfo = user.toProfileUserUiModel(),
                streak = streakData,
                featureAccess = featureAccess,
                isSubscriptionsEnabled = !hasPremiumAccess,
                shouldShowSubscriptionUI = !hasPremiumAccess,
                profileStats = profileStats,
                subscriptionStatus = featureAccess?.userAccess.toSubscriptionStatus(),
                credits = credits,
            )
        )
    }

    /**
     * A payment problem needs action, so it outranks everything. Canceled is checked before trial:
     * a canceled trial won't charge, which is what matters. The backend clears the pause date when
     * the subscription resumes.
     */
    private fun UserFeatureAccess?.toSubscriptionStatus(): ProfileSubscriptionStatus {
        if (this == null) return ProfileSubscriptionStatus.Free
        val pausedUntil = pauseResumesAtMillis
        if (!hasPremiumAccess) {
            return pausedUntil
                ?.let { ProfileSubscriptionStatus.Paused(resumesOn = EpochDateFormatter.toMediumDate(it)) }
                ?: ProfileSubscriptionStatus.Free
        }
        val endsAt = expiresAtMillis
        return when {
            premiumSource == PremiumSource.STORE && hasBillingIssue -> ProfileSubscriptionStatus.PaymentIssue
            premiumSource == PremiumSource.STORE && !willRenew && endsAt != null ->
                ProfileSubscriptionStatus.Canceled(accessUntil = EpochDateFormatter.toMediumDate(endsAt))
            isTrial -> ProfileSubscriptionStatus.Trial
            else -> ProfileSubscriptionStatus.Premium
        }
    }

    private fun AuthUser.toProfileUserUiModel(): ProfileUserUiModel {
        return ProfileUserUiModel(
            name = this.name,
            email = this.email,
            displayAlias = this.displayAlias
        )
    }
}

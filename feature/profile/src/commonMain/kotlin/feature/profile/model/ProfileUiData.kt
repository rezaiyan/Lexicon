package feature.profile.model

import domain.streak.model.StreakData
import domain.auth.model.FeatureAccessResponse

data class ProfileUiData(
    val userInfo: ProfileUserUiModel?,
    val streak: StreakData?,
    val featureAccess: FeatureAccessResponse?,
    val isSubscriptionsEnabled: Boolean,
    val shouldShowSubscriptionUI: Boolean,
    val profileStats: ProfileStatsUiModel? = null,
    val subscriptionStatus: ProfileSubscriptionStatus = ProfileSubscriptionStatus.Free,
)

/** Subscription state shown on the profile header. */
sealed interface ProfileSubscriptionStatus {
    data object Free : ProfileSubscriptionStatus
    data object Trial : ProfileSubscriptionStatus
    data object Premium : ProfileSubscriptionStatus

    /** Store subscription set not to renew; premium stays on until [accessUntil]. */
    data class Canceled(val accessUntil: String) : ProfileSubscriptionStatus

    /** The store couldn't charge the renewal; premium may still be on during the grace period. */
    data object PaymentIssue : ProfileSubscriptionStatus

    /** Store subscription paused by the user; premium is off until [resumesOn]. */
    data class Paused(val resumesOn: String) : ProfileSubscriptionStatus
}

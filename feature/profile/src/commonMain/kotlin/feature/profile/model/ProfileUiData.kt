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
enum class ProfileSubscriptionStatus {
    Free,
    Trial,
    Premium,

    /** Store subscription still active but set not to renew. */
    Cancelling,
}

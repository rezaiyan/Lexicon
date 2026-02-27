package presentation.model

import domain.streak.model.StreakData
import domain.auth.model.FeatureAccessResponse

data class ProfileUiData(
    val userInfo: ProfileUserUiModel?,
    val streak: StreakData?,
    val featureAccess: FeatureAccessResponse?,
    val isSubscriptionsEnabled: Boolean,
    val shouldShowSubscriptionUI: Boolean,
    val profileStats: ProfileStatsUiModel? = null
)

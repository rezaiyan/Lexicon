package domain.auth.model

import kotlinx.serialization.Serializable

/**
 * Global feature flags from server
 */
@Serializable
data class FeatureFlags(
    val premiumFeaturesEnabled: Boolean = false,
    val aiImageExtractionEnabled: Boolean = false,
    val aiDailyInsightEnabled: Boolean = false,
    val pushNotificationsEnabled: Boolean = false,
    val subscriptionsEnabled: Boolean = false
)

/**
 * User's personal feature access
 */
@Serializable
data class UserFeatureAccess(
    val hasPremiumAccess: Boolean = false,
    val canUseAiImageExtraction: Boolean = false,
    val canUseAiDailyInsight: Boolean = false,
    val subscriptionStatus: String = "FREE", // FREE, TRIAL, ACTIVE, EXPIRED, CANCELLED
    val subscriptionExpiresAt: String? = null,
    val aiExtractionUsageCount: Int = 0,
    val aiExtractionUsageLimit: Int = 0,
    val remainingAiExtractions: Int = 0
)

/**
 * Combined feature access response
 */
@Serializable
data class FeatureAccessResponse(
    val featureFlags: FeatureFlags,
    val userAccess: UserFeatureAccess
)



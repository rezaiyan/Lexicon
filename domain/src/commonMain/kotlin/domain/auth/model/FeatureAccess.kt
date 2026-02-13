package domain.auth.model

import kotlinx.serialization.Serializable

/**
 * Global feature flags from server
 */
@Serializable
data class FeatureFlags(
    val pushNotificationsEnabled: Boolean = true
)

/**
 * User's personal feature access
 * Simple binary premium/not-premium model
 */
@Serializable
data class UserFeatureAccess(
    val hasPremiumAccess: Boolean = false
)

/**
 * Combined feature access response
 */
@Serializable
data class FeatureAccessResponse(
    val featureFlags: FeatureFlags,
    val userAccess: UserFeatureAccess
)



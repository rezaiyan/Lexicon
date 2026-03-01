package domain.auth.model

/**
 * Domain model for authenticated user
 */
data class AuthUser(
    val id: Long,
    val email: String,
    val name: String,
    val subscriptionStatus: SubscriptionStatus = SubscriptionStatus.FREE,
    val subscriptionExpiresAt: String? = null,
    val currentStreak: Int = 0,
    val displayAlias: String? = null,
    val profileImageUrl: String? = null
)

enum class SubscriptionStatus {
    FREE,
    TRIAL,
    ACTIVE,
    EXPIRED,
    CANCELLED
}

data class AuthState(
    val isAuthenticated: Boolean = false,
    val user: AuthUser? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)


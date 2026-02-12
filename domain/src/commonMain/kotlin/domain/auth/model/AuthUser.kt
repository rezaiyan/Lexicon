package domain.auth.model

/**
 * Domain model for authenticated user
 */
data class AuthUser(
    val id: Long,
    val email: String,
    val name: String,
    val profileImageUrl: String? = null,
    val subscriptionStatus: SubscriptionStatus = SubscriptionStatus.FREE,
    val subscriptionExpiresAt: String? = null,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0
) {
    /**
     * Check if user has a generic/auto-generated name that should be updated
     */
    fun hasGenericName(): Boolean {
        // Check if name looks like email prefix or contains "privaterelay"
        return name.contains("privaterelay", ignoreCase = true) ||
               name.matches(Regex("^[a-f0-9]{6,}$")) || // Hex-like names
               name == email.substringBefore("@")
    }
    
    /**
     * Check if user should be prompted to update their profile
     */
    fun needsProfileCompletion(): Boolean {
        return hasGenericName() && name.length < 3
    }
}

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


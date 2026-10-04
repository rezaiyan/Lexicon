package domain.auth.model

import kotlinx.serialization.Serializable
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Global feature flags from server
 */
@Serializable
data class FeatureFlags(
    val pushNotificationsEnabled: Boolean = true
)

/** Why the user has premium. */
enum class PremiumSource {
    /** A store subscription/purchase (RevenueCat). Manageable in the store. */
    STORE,

    /** Granted outside the store (test users, comps). Nothing to manage or renew. */
    GRANT,
    NONE;

    companion object {
        fun from(value: String?): PremiumSource = entries.firstOrNull { it.name == value } ?: NONE
    }
}

/**
 * User's personal feature access, as decided by the backend.
 * All fields beyond [hasPremiumAccess] are optional so older/newer servers stay compatible.
 */
@Serializable
data class UserFeatureAccess(
    val hasPremiumAccess: Boolean = false,
    /** Raw [PremiumSource] name; kept as String so an unknown future value can't break parsing. */
    val source: String = PremiumSource.NONE.name,
    /** ISO-8601 end of the paid period or grant; null for lifetime or no premium. */
    val expiresAt: String? = null,
    val willRenew: Boolean = false,
    val isTrial: Boolean = false,
    /**
     * ISO-8601 time a Google Play pause ends. Set while a pause is scheduled (premium still on until
     * [expiresAt]) and while paused (no premium); null otherwise.
     */
    val pauseResumesAt: String? = null,
    /** Renewal payment failed; premium continues through the store's grace period until [expiresAt]. */
    val hasBillingIssue: Boolean = false,
) {
    /** Premium with no/unknown source (older server) is treated as GRANT: active, nothing to manage. */
    val premiumSource: PremiumSource
        get() = when {
            !hasPremiumAccess -> PremiumSource.NONE
            else -> PremiumSource.from(source).takeIf { it != PremiumSource.NONE } ?: PremiumSource.GRANT
        }

    @OptIn(ExperimentalTime::class)
    val expiresAtMillis: Long?
        get() = expiresAt?.let { Instant.parseOrNull(it)?.toEpochMilliseconds() }

    @OptIn(ExperimentalTime::class)
    val pauseResumesAtMillis: Long?
        get() = pauseResumesAt?.let { Instant.parseOrNull(it)?.toEpochMilliseconds() }
}

/**
 * Combined feature access response
 */
@Serializable
data class FeatureAccessResponse(
    val featureFlags: FeatureFlags,
    val userAccess: UserFeatureAccess
)

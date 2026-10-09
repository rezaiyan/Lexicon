package data.credits.remote.model

import kotlinx.serialization.Serializable

/**
 * `GET /credits`. Map keys are action and tier names; unknown ones are ignored when mapping, so the
 * server can add actions or tiers without breaking this app version.
 */
@Serializable
data class CreditBalanceDto(
    val allowanceRemaining: Int = 0,
    val monthlyAllowance: Int = 0,
    val bonusBalance: Int = 0,
    /** ISO-8601 instant the allowance resets. */
    val periodEndsAt: String? = null,
    val tier: String = "FREE",
    val costs: Map<String, Int> = emptyMap(),
    val monthlyAllowances: Map<String, Int> = emptyMap(),
)

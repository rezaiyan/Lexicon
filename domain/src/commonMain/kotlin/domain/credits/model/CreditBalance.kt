package domain.credits.model

/** Something that spends AI credits. Mirrors the server's action names. */
enum class CreditAction { PHOTO_EXTRACTION, AI_SUGGESTION, TEXT_TRANSLATION }

/** The plan a monthly allowance is sized for. */
enum class CreditTier { FREE, TRIAL, PREMIUM }

/**
 * The signed-in user's AI credits, as the server last reported them. The server is the only
 * authority: this is for display and for skipping a request that would be refused anyway.
 *
 * Costs and allowances come from the server too, so pricing changes need no app release.
 */
data class CreditBalance(
    /** Monthly allowance left; restored in full (never rolled over) at [periodEndsAtMillis]. */
    val allowanceRemaining: Int,
    val monthlyAllowance: Int,
    /** Signup bonus and grants; never expires. */
    val bonusBalance: Int,
    val periodEndsAtMillis: Long?,
    val tier: CreditTier,
    val costs: Map<CreditAction, Int>,
    val monthlyAllowances: Map<CreditTier, Int>,
) {
    val balance: Int get() = allowanceRemaining + bonusBalance

    /** An action the server didn't price is free. */
    fun costOf(action: CreditAction): Int = costs[action] ?: 0

    fun canAfford(action: CreditAction): Boolean = balance >= costOf(action)

    /** What premium includes per month, for upgrade prompts; null if the server didn't say. */
    val premiumMonthlyAllowance: Int? get() = monthlyAllowances[CreditTier.PREMIUM]
}

package fakes

import core.common.Try
import domain.credits.ICreditsRepository
import domain.credits.model.CreditAction
import domain.credits.model.CreditBalance
import domain.credits.model.CreditTier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * In-memory credits. [refresh] publishes [serverBalance] (what the server would answer now), so a
 * test can change it and check that a screen re-reads it.
 */
class FakeCreditsRepository(initial: CreditBalance? = null) : ICreditsRepository {
    val balance = MutableStateFlow(initial)
    var serverBalance: CreditBalance? = initial
    var failure: Throwable? = null
    var refreshCount = 0
        private set

    override fun observeBalance(): Flow<CreditBalance?> = balance

    override suspend fun refresh(): Try<CreditBalance> {
        refreshCount++
        failure?.let { return Try.failure(it) }
        val current = serverBalance ?: return Try.failure(IllegalStateException("no server balance"))
        balance.value = current
        return Try.success(current)
    }
}

fun creditBalance(
    allowanceRemaining: Int = 5,
    bonusBalance: Int = 15,
    tier: CreditTier = CreditTier.FREE,
    periodEndsAtMillis: Long? = 1_800_000_000_000,
    costs: Map<CreditAction, Int> = mapOf(CreditAction.PHOTO_EXTRACTION to 3, CreditAction.AI_SUGGESTION to 1),
) = CreditBalance(
    allowanceRemaining = allowanceRemaining,
    monthlyAllowance = 5,
    bonusBalance = bonusBalance,
    periodEndsAtMillis = periodEndsAtMillis,
    tier = tier,
    costs = costs,
    monthlyAllowances = mapOf(CreditTier.FREE to 5, CreditTier.TRIAL to 30, CreditTier.PREMIUM to 300),
)

package data.credits.mapper

import data.credits.remote.model.CreditBalanceDto
import domain.credits.model.CreditAction
import domain.credits.model.CreditBalance
import domain.credits.model.CreditTier
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
fun CreditBalanceDto.toDomain(): CreditBalance = CreditBalance(
    allowanceRemaining = allowanceRemaining,
    monthlyAllowance = monthlyAllowance,
    bonusBalance = bonusBalance,
    periodEndsAtMillis = periodEndsAt?.let { Instant.parseOrNull(it)?.toEpochMilliseconds() },
    tier = enumOrNull<CreditTier>(tier) ?: CreditTier.FREE,
    costs = costs.keyedBy<CreditAction>(),
    monthlyAllowances = monthlyAllowances.keyedBy<CreditTier>(),
)

private inline fun <reified E : Enum<E>> enumOrNull(name: String): E? = enumValues<E>().firstOrNull { it.name == name }

/** Keeps the entries whose key this app version knows. */
private inline fun <reified E : Enum<E>> Map<String, Int>.keyedBy(): Map<E, Int> =
    mapNotNull { (key, value) -> enumOrNull<E>(key)?.let { it to value } }.toMap()

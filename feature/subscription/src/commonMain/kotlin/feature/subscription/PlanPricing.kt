package feature.subscription

import androidx.compose.runtime.Immutable
import domain.subscription.model.PackagePeriod
import domain.subscription.model.SubscriptionPackage
import feature.subscription.model.TrialSchedule

/** One purchasable plan, with the price facts the paywall shows next to it. */
@Immutable
data class PlanOption(
    val pkg: SubscriptionPackage,
    /** Store-formatted price for the whole billing period. */
    val price: String,
    /** Annual price spread over 12 months, in the store's currency format; null for monthly. */
    val perMonthPrice: String?,
    /** How much cheaper annual is than 12 × monthly, floored; null when unknown or negligible. */
    val savingsPercent: Int?,
    val trialDays: Int?,
    /** Real dates for [trialDays]; filled in by [SubscriptionContentFactory]. */
    val trialSchedule: TrialSchedule? = null,
) {
    val identifier: String get() = pkg.identifier
    val period: PackagePeriod get() = pkg.packagePeriod
}

/**
 * Pure pricing rules for the paywall: which plans to offer, what they really cost per month and
 * how much annual saves. Kept out of the UI so the numbers shown to users are unit-tested.
 */
object PlanPricing {

    private const val MIN_ADVERTISED_SAVINGS_PERCENT = 5
    private const val MONTHS_PER_YEAR = 12
    private const val MICROS_PER_UNIT = 1_000_000L

    fun options(packages: List<SubscriptionPackage>): List<PlanOption> {
        val monthlyMicros = packages
            .firstOrNull { it.packagePeriod == PackagePeriod.MONTHLY }
            ?.product?.priceAmountMicros
        return packages
            .filter { it.packagePeriod == PackagePeriod.ANNUAL || it.packagePeriod == PackagePeriod.MONTHLY }
            .sortedBy { if (it.packagePeriod == PackagePeriod.ANNUAL) 0 else 1 }
            .map { pkg ->
                val micros = pkg.product.priceAmountMicros
                val isAnnual = pkg.packagePeriod == PackagePeriod.ANNUAL
                PlanOption(
                    pkg = pkg,
                    price = pkg.product.priceFormatted,
                    perMonthPrice = micros
                        ?.takeIf { isAnnual }
                        ?.let { formatLike(pkg.product.priceFormatted, it / MONTHS_PER_YEAR) },
                    savingsPercent = if (isAnnual) savingsPercent(micros, monthlyMicros) else null,
                    trialDays = pkg.trialPeriodDays?.takeIf { pkg.hasFreeTrial && it > 0 },
                )
            }
    }

    fun defaultSelection(options: List<PlanOption>): String? =
        (options.firstOrNull { it.period == PackagePeriod.ANNUAL } ?: options.firstOrNull())?.identifier

    /**
     * The plan an active entitlement was bought from. Android reports "subId" on the entitlement but
     * "subId:basePlan" on the product, so fall back to the subscription id when that is unambiguous.
     */
    fun matchingOption(options: List<PlanOption>, entitlementProductId: String?): PlanOption? {
        if (entitlementProductId.isNullOrBlank()) return null
        options.firstOrNull { it.pkg.product.productIdentifier == entitlementProductId }?.let { return it }
        val baseId = entitlementProductId.substringBefore(':')
        return options
            .filter { it.pkg.product.productIdentifier.substringBefore(':') == baseId }
            .singleOrNull()
    }

    private fun savingsPercent(annualMicros: Long?, monthlyMicros: Long?): Int? {
        if (annualMicros == null || monthlyMicros == null || monthlyMicros <= 0) return null
        val yearOfMonthly = monthlyMicros * MONTHS_PER_YEAR
        val percent = ((yearOfMonthly - annualMicros) * 100 / yearOfMonthly).toInt()
        return percent.takeIf { it >= MIN_ADVERTISED_SAVINGS_PERCENT }
    }

    /**
     * Writes [amountMicros] in the same currency format as [template] (a store-formatted price):
     * same symbol position and decimal separator. Digits after the last separator count as decimals
     * only when there are one or two of them; "¥3,000" is grouping, so the result has no decimals.
     * Grouping separators are not reproduced. Returns null when [template] has no number in it.
     */
    fun formatLike(template: String, amountMicros: Long): String? {
        val first = template.indexOfFirst { it.isDigit() }
        val last = template.indexOfLast { it.isDigit() }
        if (first < 0) return null
        val number = template.substring(first, last + 1)
        val separatorIndex = number.indexOfLast { it == '.' || it == ',' }
        val decimals = if (separatorIndex >= 0) number.length - separatorIndex - 1 else 0
        val fractionDigits = if (decimals in 1..2) decimals else 0

        val unitScale = pow10(fractionDigits)
        val units = (amountMicros * unitScale + MICROS_PER_UNIT / 2) / MICROS_PER_UNIT
        val whole = units / unitScale
        val amount = if (fractionDigits == 0) {
            whole.toString()
        } else {
            val fraction = (units % unitScale).toString().padStart(fractionDigits, '0')
            "$whole${number[separatorIndex]}$fraction"
        }
        return template.substring(0, first) + amount + template.substring(last + 1)
    }

    private fun pow10(exponent: Int): Long {
        var result = 1L
        repeat(exponent) { result *= 10 }
        return result
    }
}

package feature.subscription

import domain.auth.model.PremiumSource
import domain.auth.model.UserFeatureAccess
import domain.common.util.EpochDateFormatter
import domain.subscription.model.PackagePeriod
import domain.subscription.model.SubscriptionCustomerInfo
import domain.subscription.model.SubscriptionPackage
import feature.subscription.model.Membership
import feature.subscription.model.MembershipStatus
import feature.subscription.model.SubscriptionContent
import feature.subscription.model.TrialSchedule
import kotlin.time.Clock

/**
 * Turns store and backend facts into what the subscription screen shows. Pure apart from the
 * injected clock and date formatter, so every state the user can be in is unit-tested here.
 */
class SubscriptionContentFactory(
    private val nowMillis: () -> Long = { Clock.System.now().toEpochMilliseconds() },
    private val formatDate: (Long) -> String = { EpochDateFormatter.toMediumDate(it) },
) {

    /**
     * Premium from any source shows the membership; otherwise the paywall. The store entitlement
     * on this device is the freshest source; the backend covers purchases made on another
     * platform and grants given outside the store.
     */
    fun content(
        packages: List<SubscriptionPackage>,
        customerInfo: SubscriptionCustomerInfo?,
        access: UserFeatureAccess,
    ): SubscriptionContent {
        val plans = PlanPricing.options(packages)
        val isMember = access.hasPremiumAccess || customerInfo?.isSubscribed == true
        val pauseResumesAt = access.pauseResumesAtMillis?.takeIf { it > nowMillis() }
        return when {
            isMember -> SubscriptionContent.Member(membership(plans, customerInfo, access, pauseResumesAt))
            pauseResumesAt != null -> SubscriptionContent.Paused(
                resumesOn = formatDate(pauseResumesAt),
                daysLeft = daysUntil(pauseResumesAt),
            )
            else -> SubscriptionContent.Paywall(
                plans.map { it.copy(trialSchedule = it.trialDays?.let(::trialSchedule)) }
            )
        }
    }

    private fun membership(
        plans: List<PlanOption>,
        customerInfo: SubscriptionCustomerInfo?,
        access: UserFeatureAccess,
        pauseResumesAt: Long?,
    ): Membership {
        val entitlement = customerInfo?.primaryEntitlement
        val plan = PlanPricing.matchingOption(plans, entitlement?.productIdentifier)
        val period = plan?.period ?: entitlement?.productIdentifier?.let(::periodFromProductId)
        val status = when {
            // The store SDK on this device is freshest for billing; only the backend knows about pauses.
            entitlement != null -> status(
                StoreFacts(
                    expiresAtMillis = entitlement.expirationDateMillis,
                    willRenew = entitlement.willRenew,
                    isTrial = entitlement.isInTrial,
                    hasBillingIssue = entitlement.billingIssueDetectedAtMillis != null,
                    pauseResumesAt = pauseResumesAt,
                ),
                period,
            )
            access.premiumSource == PremiumSource.STORE -> status(
                StoreFacts(
                    expiresAtMillis = access.expiresAtMillis,
                    willRenew = access.willRenew,
                    isTrial = access.isTrial,
                    hasBillingIssue = access.hasBillingIssue,
                    pauseResumesAt = pauseResumesAt,
                ),
                period,
            )
            else -> MembershipStatus.Granted(until = access.expiresAtMillis?.let(formatDate))
        }
        return Membership(
            period = period,
            price = plan?.price,
            status = status,
            isManageable = customerInfo?.isSubscribed == true,
        )
    }

    /** What the store says about one subscription, from whichever source knows it. */
    private data class StoreFacts(
        val expiresAtMillis: Long?,
        val willRenew: Boolean,
        val isTrial: Boolean,
        val hasBillingIssue: Boolean,
        val pauseResumesAt: Long?,
    )

    /**
     * Order matters. Stores report willRenew = false for a payment problem and for a scheduled
     * pause too, so both are checked before "canceled". Canceled wins over trial: a canceled trial
     * will not charge, which is what the user must see.
     */
    private fun status(facts: StoreFacts, period: PackagePeriod?): MembershipStatus {
        val expiresAtMillis = facts.expiresAtMillis ?: return MembershipStatus.Lifetime
        val isTrial = facts.isTrial
        val date = formatDate(expiresAtMillis)
        val daysLeft = daysUntil(expiresAtMillis)
        return when {
            facts.hasBillingIssue -> MembershipStatus.BillingIssue(accessEndsOn = date, daysLeft = daysLeft)
            facts.pauseResumesAt != null -> MembershipStatus.PauseScheduled(
                pausesOn = date,
                resumesOn = formatDate(facts.pauseResumesAt),
            )
            !facts.willRenew -> MembershipStatus.Canceled(
                accessEndsOn = date,
                daysLeft = daysLeft,
                wasTrial = isTrial,
                remainingFraction = period
                    ?.takeUnless { isTrial }
                    ?.periodDays()
                    ?.let { (daysLeft.toFloat() / it).coerceIn(0f, 1f) },
            )
            isTrial -> MembershipStatus.Trial(endsOn = date, daysLeft = daysLeft)
            else -> MembershipStatus.Renewing(renewsOn = date)
        }
    }

    /** Stores need 24h notice, so the last free day is the day before the first charge. */
    private fun trialSchedule(trialDays: Int): TrialSchedule {
        val now = nowMillis()
        return TrialSchedule(
            lastFreeDay = formatDate(now + (trialDays - 1).coerceAtLeast(0) * MILLIS_PER_DAY),
            chargeDate = formatDate(now + trialDays * MILLIS_PER_DAY),
        )
    }

    /** Whole days left; under a day counts as 0 ("last day"). */
    private fun daysUntil(epochMillis: Long): Int =
        ((epochMillis - nowMillis()) / MILLIS_PER_DAY).coerceAtLeast(0L).toInt()

    private fun PackagePeriod.periodDays(): Int? = when (this) {
        PackagePeriod.ANNUAL -> DAYS_PER_YEAR
        PackagePeriod.MONTHLY -> DAYS_PER_MONTH
        PackagePeriod.LIFETIME, PackagePeriod.UNKNOWN -> null
    }

    /** Fallback when offerings didn't load: infer the period from common store product id shapes. */
    private fun periodFromProductId(productId: String): PackagePeriod? {
        val id = productId.lowercase()
        return when {
            "annual" in id || "year" in id || "p1y" in id -> PackagePeriod.ANNUAL
            "month" in id || "p1m" in id -> PackagePeriod.MONTHLY
            else -> null
        }
    }

    private companion object {
        const val MILLIS_PER_DAY = 24 * 60 * 60 * 1000L
        const val DAYS_PER_YEAR = 365
        const val DAYS_PER_MONTH = 30
    }
}

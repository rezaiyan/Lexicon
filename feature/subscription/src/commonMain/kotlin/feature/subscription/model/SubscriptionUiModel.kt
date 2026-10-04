package feature.subscription.model

import androidx.compose.runtime.Immutable
import core.common.UiState
import domain.subscription.model.PackagePeriod
import feature.subscription.PlanOption

data class SubscriptionScreenState(
    val content: UiState<SubscriptionContent> = UiState.Loading,
    /** Plan the paywall CTA will buy; defaults to annual once offerings load. */
    val selectedPlanId: String? = null,
    val isPurchasing: Boolean = false,
)

/** What the screen shows: the paywall for free users, the membership for premium users. */
@Immutable
sealed interface SubscriptionContent {

    data class Paywall(val plans: List<PlanOption>) : SubscriptionContent

    data class Member(val membership: Membership) : SubscriptionContent

    /**
     * Google Play subscription on pause: no premium now, but it is still the user's subscription
     * and resumes by itself on [resumesOn]. Shown instead of the paywall so nobody buys twice.
     */
    data class Paused(val resumesOn: String, val daysLeft: Int) : SubscriptionContent
}

/** A premium membership as the user should understand it. */
@Immutable
data class Membership(
    /** Billing period of the plan, when known; drives the plan name. */
    val period: PackagePeriod?,
    /** Store price for one billing period, when the plan could be matched to an offering. */
    val price: String?,
    val status: MembershipStatus,
    /** Bought through the store on this account, so the store can manage it. */
    val isManageable: Boolean,
)

/**
 * Where a membership stands. Each state carries exactly the facts its UI needs, so impossible
 * combinations (a lifetime plan that "renews", a canceled plan with a charge) can't be expressed.
 */
@Immutable
sealed interface MembershipStatus {

    /** Paid and auto-renewing: the next event is a charge. */
    data class Renewing(val renewsOn: String) : MembershipStatus

    /** Free trial that converts to paid on [endsOn] unless canceled. */
    data class Trial(val endsOn: String, val daysLeft: Int) : MembershipStatus

    /**
     * Auto-renew turned off but the paid (or trial) period hasn't run out: premium stays fully on
     * until [accessEndsOn], then the account drops to free. Nothing more will be charged.
     */
    data class Canceled(
        val accessEndsOn: String,
        val daysLeft: Int,
        val wasTrial: Boolean,
        /** Share of the billing period still left, 0..1; null when the period length is unknown. */
        val remainingFraction: Float?,
    ) : MembershipStatus

    /**
     * A renewal payment failed. The store retries during its grace period and premium stays on
     * until [accessEndsOn] (null when unknown); fixing the payment method keeps it going.
     */
    data class BillingIssue(val accessEndsOn: String?, val daysLeft: Int?) : MembershipStatus

    /**
     * A Google Play pause is scheduled: premium runs until [pausesOn], nothing is charged during
     * the pause, and the subscription resumes (and renews) on [resumesOn].
     */
    data class PauseScheduled(val pausesOn: String, val resumesOn: String) : MembershipStatus

    /** Premium given outside the store (comp, test user); nothing to pay or manage. */
    data class Granted(val until: String?) : MembershipStatus

    /** Store purchase with no expiry. */
    data object Lifetime : MembershipStatus
}

/** Real dates for a plan's free trial, shown before the user commits. */
@Immutable
data class TrialSchedule(
    /** Last day to cancel without being charged (stores require 24h notice). */
    val lastFreeDay: String,
    val chargeDate: String,
)

/** One-shot feedback for actions; shown as a snackbar. */
sealed interface SubscriptionEffect {
    data object PurchasesRestored : SubscriptionEffect
    data object NothingToRestore : SubscriptionEffect

    /** [message] is a user message or a message key localized by the screen. */
    data class Failure(val message: String) : SubscriptionEffect
}

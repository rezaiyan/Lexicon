package feature.subscription.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import domain.subscription.model.PackagePeriod
import domain.subscription.model.SubscriptionPackage
import domain.subscription.model.SubscriptionProduct
import feature.subscription.PlanPricing
import feature.subscription.model.Membership
import feature.subscription.model.MembershipStatus
import feature.subscription.model.TrialSchedule
import theme.LexiconTheme
import theme.Theme

private val previewPlans = PlanPricing.options(
    listOf(
        SubscriptionPackage(
            identifier = "monthly",
            packagePeriod = PackagePeriod.MONTHLY,
            product = SubscriptionProduct("Monthly", "", "$4.99", 4_990_000, "vokab_monthly"),
        ),
        SubscriptionPackage(
            identifier = "annual",
            packagePeriod = PackagePeriod.ANNUAL,
            product = SubscriptionProduct("Annual", "", "$29.99", 29_990_000, "vokab_annual"),
            trialPeriodDays = 7,
            hasFreeTrial = true,
        ),
    )
).map { it.copy(trialSchedule = it.trialDays?.let { TrialSchedule("Oct 9, 2026", "Oct 10, 2026") }) }

@Preview(showBackground = true, heightDp = 1800)
@Composable
private fun PaywallPreview() {
    LexiconTheme {
        Surface {
            var selected by remember { mutableStateOf<String?>("annual") }
            Column(Modifier.verticalScroll(rememberScrollState()).padding(Theme.spacing.md)) {
                SubscriptionNotSubscribedContent(
                    plans = previewPlans,
                    selectedPlanId = selected,
                    isPurchasing = false,
                    onSelectPlan = { selected = it },
                    onPurchase = {},
                    onRestoreClick = {},
                )
            }
        }
    }
}

private class MembershipStatusProvider : PreviewParameterProvider<MembershipStatus> {
    override val values = sequenceOf(
        MembershipStatus.Renewing(renewsOn = "Oct 3, 2027"),
        MembershipStatus.Trial(endsOn = "Oct 10, 2026", daysLeft = 5),
        MembershipStatus.Canceled(
            accessEndsOn = "Dec 14",
            daysLeft = 72,
            wasTrial = false,
            remainingFraction = 0.2f,
        ),
        MembershipStatus.Canceled(
            accessEndsOn = "Oct 5",
            daysLeft = 2,
            wasTrial = true,
            remainingFraction = null,
        ),
        MembershipStatus.BillingIssue(accessEndsOn = "Oct 20", daysLeft = 16),
        MembershipStatus.PauseScheduled(pausesOn = "Nov 3", resumesOn = "Jan 3, 2027"),
        MembershipStatus.Granted(until = null),
    )
}

@Preview(showBackground = true, heightDp = 1400)
@Composable
private fun MembershipPreview(@PreviewParameter(MembershipStatusProvider::class) status: MembershipStatus) {
    LexiconTheme {
        Surface {
            Column(Modifier.padding(Theme.spacing.md)) {
                SubscriptionActiveContent(
                    membership = Membership(
                        period = PackagePeriod.ANNUAL,
                        price = "$29.99",
                        status = status,
                        isManageable = status !is MembershipStatus.Granted,
                    ),
                    onManage = {},
                )
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 1400)
@Composable
private fun PausedPreview() {
    LexiconTheme {
        Surface {
            Column(Modifier.padding(Theme.spacing.md)) {
                SubscriptionPausedContent(resumesOn = "Jan 3, 2027", daysLeft = 91, onResume = {})
            }
        }
    }
}

package feature.subscription.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import components.sheet.SheetPrimaryButton
import domain.subscription.model.PackagePeriod
import expects.openUrl
import feature.subscription.PlanOption
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.paywall_billing_title
import lexicon.resources.generated.resources.paywall_choose_plan
import lexicon.resources.generated.resources.paywall_cta_monthly
import lexicon.resources.generated.resources.paywall_cta_trial
import lexicon.resources.generated.resources.paywall_cta_yearly
import lexicon.resources.generated.resources.paywall_premium_unlocks
import lexicon.resources.generated.resources.paywall_reassurance
import lexicon.resources.generated.resources.paywall_reassurance_trial
import lexicon.resources.generated.resources.paywall_restore_hint
import lexicon.resources.generated.resources.paywall_unavailable
import lexicon.resources.generated.resources.privacy_policy
import lexicon.resources.generated.resources.restore_purchases
import lexicon.resources.generated.resources.subscription_terms
import lexicon.resources.generated.resources.terms_of_use
import org.jetbrains.compose.resources.stringResource
import theme.AppColors
import theme.Theme

/**
 * Paywall. Order follows the decision: what you get (hero) → pick a plan → one clear CTA with
 * its exact consequence → proof (trial timeline, features) → fine print. Plans sit above the
 * fold; nothing about price or renewal is left for the store sheet to reveal.
 */
@Composable
fun SubscriptionNotSubscribedContent(
    plans: List<PlanOption>,
    selectedPlanId: String?,
    isPurchasing: Boolean,
    monthlyAiCredits: Int?,
    onSelectPlan: (String) -> Unit,
    onPurchase: () -> Unit,
    onRestoreClick: () -> Unit,
) {
    val selected = plans.firstOrNull { it.identifier == selectedPlanId }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = Theme.spacing.xxxl),
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.lg),
    ) {
        PremiumHeroSection()

        if (plans.isEmpty()) {
            Text(
                text = stringResource(Res.string.paywall_unavailable),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.sm)) {
                Text(
                    text = stringResource(Res.string.paywall_choose_plan),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                PlanSelector(
                    plans = plans,
                    selectedPlanId = selectedPlanId,
                    enabled = !isPurchasing,
                    onSelect = onSelectPlan,
                )
            }

            selected?.let { plan ->
                PurchaseCallToAction(plan = plan, isPurchasing = isPurchasing, onPurchase = onPurchase)
            }

            // AnimatedContent keeps the outgoing plan's timeline while it animates away.
            AnimatedContent(
                targetState = selected,
                contentKey = { it?.trialSchedule },
                label = "trialTimeline",
            ) { plan ->
                val trial = plan?.trialSchedule
                if (trial != null) {
                    TrialTimeline(lastFreeDate = trial.lastFreeDay, chargeDate = trial.chargeDate, price = plan.price)
                }
            }
        }

        PremiumFeatureList(
            title = stringResource(Res.string.paywall_premium_unlocks),
            monthlyAiCredits = monthlyAiCredits,
        )

        FreePlanNote()

        BillingFinePrint()

        RestoreRow(enabled = !isPurchasing, onRestoreClick = onRestoreClick)

        SubscriptionLegalLinks()
    }
}

@Composable
private fun PurchaseCallToAction(plan: PlanOption, isPurchasing: Boolean, onPurchase: () -> Unit) {
    val trialDays = plan.trialDays
    val label = when {
        trialDays != null -> stringResource(Res.string.paywall_cta_trial, trialDays)
        plan.period == PackagePeriod.ANNUAL -> stringResource(Res.string.paywall_cta_yearly, plan.price)
        else -> stringResource(Res.string.paywall_cta_monthly, plan.price)
    }
    val trial = plan.trialSchedule
    val reassurance = if (trial != null) {
        stringResource(Res.string.paywall_reassurance_trial, trial.lastFreeDay)
    } else {
        stringResource(Res.string.paywall_reassurance, storeName())
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.xs),
    ) {
        SheetPrimaryButton(
            text = label,
            onClick = onPurchase,
            isLoading = isPurchasing,
            containerColor = AppColors.subscriptionPremiumAccent,
            contentColor = Color.White,
        )
        Text(
            text = reassurance,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun BillingFinePrint() {
    Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.xxs)) {
        Text(
            text = stringResource(Res.string.paywall_billing_title),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(Res.string.subscription_terms),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RestoreRow(enabled: Boolean, onRestoreClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(Res.string.paywall_restore_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = onRestoreClick, enabled = enabled) {
            Text(stringResource(Res.string.restore_purchases), fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun SubscriptionLegalLinks() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        LegalLink(stringResource(Res.string.terms_of_use)) { openUrl("https://alirezaiyan.com/vokab/terms") }
        Text(
            text = " • ",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LegalLink(stringResource(Res.string.privacy_policy)) { openUrl("https://alirezaiyan.com/vokab/privacy") }
    }
}

@Composable
private fun LegalLink(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.primary,
        textDecoration = TextDecoration.Underline,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = Theme.spacing.sm, vertical = Theme.spacing.xs),
    )
}

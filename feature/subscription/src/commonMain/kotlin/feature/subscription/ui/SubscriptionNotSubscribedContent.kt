package feature.subscription.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.style.TextDecoration
import domain.subscription.model.PackagePeriod
import domain.subscription.model.SubscriptionPackage
import expects.openUrl
import org.jetbrains.compose.resources.stringResource
import theme.AppColors
import theme.Theme
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.billing_period_annual
import lexicon.resources.generated.resources.billing_period_monthly
import lexicon.resources.generated.resources.privacy_policy
import lexicon.resources.generated.resources.restore_purchases
import lexicon.resources.generated.resources.terms_of_use

@Composable
fun SubscriptionNotSubscribedContent(
    packages: List<SubscriptionPackage>,
    isPurchasing: Boolean,
    onPurchaseClick: (SubscriptionPackage) -> Unit,
    onRestoreClick: () -> Unit,
    onPackagesSectionPositioned: (Float) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = Theme.spacing.extraLarge4),
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.medium)
    ) {
        PremiumHeroSection()

        ComparisonTable()

        PremiumFeaturesGrid()

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coordinates ->
                    onPackagesSectionPositioned(coordinates.positionInRoot().y)
                }
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(Theme.spacing.medium)
            ) {
                packages.forEachIndexed { index, pkg ->
                    val billingPeriod = when (pkg.packagePeriod) {
                        PackagePeriod.MONTHLY -> stringResource(Res.string.billing_period_monthly)
                        PackagePeriod.ANNUAL -> stringResource(Res.string.billing_period_annual)
                        else -> null
                    }
                    val isRecommended = pkg.packagePeriod == PackagePeriod.ANNUAL
                    val accentColor = if (isRecommended) {
                        AppColors.subscriptionRecommended
                    } else {
                        AppColors.subscriptionStandard
                    }

                    if (billingPeriod.isNullOrBlank().not()) {
                        val subscriptionPlan = SubscriptionPlan(
                            title = pkg.product.title,
                            billingPeriod = billingPeriod,
                            description = pkg.product.description,
                            price = pkg.product.priceFormatted,
                            accentColor = accentColor,
                            hasFreeTrial = pkg.hasFreeTrial,
                            trialPeriodDays = pkg.trialPeriodDays
                        )

                        PlanCard(
                            plan = subscriptionPlan,
                            isRecommended = isRecommended,
                            isPurchasing = isPurchasing,
                            onClick = { onPurchaseClick(pkg) }
                        )
                    }
                }
            }
        }

        SubscriptionLegalLinks()

        OutlinedButton(
            onClick = onRestoreClick,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isPurchasing
        ) {
            Text(stringResource(Res.string.restore_purchases))
        }
    }
}

@Composable
private fun SubscriptionLegalLinks() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Theme.spacing.medium, vertical = Theme.spacing.small),
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(Res.string.terms_of_use),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier
                .clickable {
                    openUrl("https://alirezaiyan.com/vokab/terms")
                }
                .padding(horizontal = Theme.spacing.small)
        )
        Text(
            text = " • ",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(Res.string.privacy_policy),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier
                .clickable {
                    openUrl("https://alirezaiyan.com/vokab/privacy")
                }
                .padding(horizontal = Theme.spacing.small)
        )
    }
}

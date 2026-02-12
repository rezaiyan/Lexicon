package presentation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import presentation.model.UiState
import presentation.ui.screens.subscription.ComparisonTable
import presentation.ui.screens.subscription.PlanCard
import presentation.ui.screens.subscription.PremiumFeaturesGrid
import presentation.ui.screens.subscription.PremiumHeroSection
import presentation.ui.screens.subscription.SubscriptionPlan
import theme.AppColors
import theme.Theme
import theme.LexiconTheme
import vokab.resources.generated.resources.Res
import vokab.resources.generated.resources.cancel_anytime_prices_in_usd
import vokab.resources.generated.resources.restore_purchases
import vokab.resources.generated.resources.subscription_terms


@Preview(showBackground = true)
@Composable
private fun SubscriptionPlansPreview() {
    LexiconTheme {
        val plans = listOf(
            SubscriptionPlan(
                title = "Vokab Pro",
                billingPeriod = "Monthly",
                price = "$2.49",
                description = "Learn faster with AI imports and access ready-made vocab collections.",
                accentColor = AppColors.subscriptionStandard
            ),
            SubscriptionPlan(
                title = "Vokab Pro",
                billingPeriod = "Yearly",
                price = "$16.99",
                description = "Learn faster with AI imports and access ready-made vocab collections.",
                accentColor = AppColors.subscriptionRecommended
            )
        )

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            color = Color(0xFFF5F3FF)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(Theme.spacing.medium),
                verticalArrangement = Arrangement.spacedBy(Theme.spacing.medium)
            ) {
                plans.forEachIndexed { index, plan ->
                    PlanCard(
                        plan = plan,
                        isRecommended = index == 1,
                        onClick = {}
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Subscription Screen - Full")
@Composable
private fun SubscriptionScreenPreview() {
    LexiconTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(Theme.spacing.medium),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(Theme.spacing.medium)
            ) {
//                PremiumHeroSection()
//                ComparisonTable()
                PremiumFeaturesGrid()
                
                OutlinedButton(
                    onClick = {},
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(Res.string.restore_purchases))
                }
                
                Text(
                    text = stringResource(Res.string.cancel_anytime_prices_in_usd),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Theme.spacing.extraSmall)
                )
                
                Text(
                    text = stringResource(Res.string.subscription_terms),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Theme.spacing.medium)
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Premium Hero Section")
@Composable
private fun PremiumHeroSectionPreview() {
    LexiconTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(Theme.spacing.medium),
            contentAlignment = Alignment.Center
        ) {
            PremiumHeroSection()
        }
    }
}

@Preview(showBackground = true, name = "Comparison Table")
@Composable
private fun ComparisonTablePreview() {
    LexiconTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(Theme.spacing.medium),
            contentAlignment = Alignment.Center
        ) {
            ComparisonTable()
        }
    }
}

@Preview(showBackground = true, name = "Premium Features Grid")
@Composable
private fun PremiumFeaturesGridPreview() {
    LexiconTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(Theme.spacing.medium),
            contentAlignment = Alignment.Center
        ) {
            PremiumFeaturesGrid()
        }
    }
}
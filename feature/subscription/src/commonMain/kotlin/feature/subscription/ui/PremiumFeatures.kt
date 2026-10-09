package feature.subscription.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import components.animation.staggeredFadeSlide
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.paywall_feature_ai_credits
import lexicon.resources.generated.resources.paywall_feature_ai_credits_desc
import lexicon.resources.generated.resources.paywall_feature_ai_credits_generic
import lexicon.resources.generated.resources.paywall_feature_export
import lexicon.resources.generated.resources.paywall_feature_export_desc
import lexicon.resources.generated.resources.paywall_feature_insights
import lexicon.resources.generated.resources.paywall_feature_insights_desc
import lexicon.resources.generated.resources.paywall_feature_word_rush
import lexicon.resources.generated.resources.paywall_feature_word_rush_desc
import lexicon.resources.generated.resources.paywall_free_forever
import lexicon.resources.generated.resources.paywall_free_forever_desc
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import theme.AppColors
import theme.Theme

@Immutable
private data class PremiumFeature(
    val icon: ImageVector,
    val title: StringResource,
    val description: StringResource,
)

/**
 * Exactly what premium adds — keep in sync with the real gates: the monthly AI credit allowance
 * (server `app.credits`, shown first), then the premium-only features (AiController insight,
 * Insights tab, Word Rush card, word export).
 */
private val premiumOnlyFeatures = listOf(
    PremiumFeature(
        Icons.Outlined.BarChart,
        Res.string.paywall_feature_insights,
        Res.string.paywall_feature_insights_desc,
    ),
    PremiumFeature(
        Icons.Outlined.Bolt,
        Res.string.paywall_feature_word_rush,
        Res.string.paywall_feature_word_rush_desc,
    ),
    PremiumFeature(
        Icons.Outlined.FileUpload,
        Res.string.paywall_feature_export,
        Res.string.paywall_feature_export_desc,
    ),
)

/**
 * Premium feature list: the monthly AI credit allowance, then the premium-only features. With
 * [unlocked] each row carries a check, for subscribers reviewing what their plan includes.
 *
 * @param monthlyAiCredits credits premium includes per month (from the server); a generic title
 *   is shown while unknown.
 */
@Composable
fun PremiumFeatureList(
    title: String,
    monthlyAiCredits: Int?,
    modifier: Modifier = Modifier,
    unlocked: Boolean = false,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Theme.spacing.sm)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(Theme.shapes.large))
                .padding(vertical = Theme.spacing.xs),
        ) {
            PremiumFeatureRow(
                icon = Icons.Outlined.AutoAwesome,
                title = monthlyAiCredits
                    ?.let { stringResource(Res.string.paywall_feature_ai_credits, it) }
                    ?: stringResource(Res.string.paywall_feature_ai_credits_generic),
                description = stringResource(Res.string.paywall_feature_ai_credits_desc),
                unlocked = unlocked,
                index = 0,
                showDivider = premiumOnlyFeatures.isNotEmpty(),
            )
            premiumOnlyFeatures.forEachIndexed { index, feature ->
                PremiumFeatureRow(
                    icon = feature.icon,
                    title = stringResource(feature.title),
                    description = stringResource(feature.description),
                    unlocked = unlocked,
                    index = index + 1,
                    showDivider = index < premiumOnlyFeatures.lastIndex,
                )
            }
        }
    }
}

@Composable
private fun PremiumFeatureRow(
    icon: ImageVector,
    title: String,
    description: String,
    unlocked: Boolean,
    index: Int,
    showDivider: Boolean,
) {
    val accent = AppColors.subscriptionPremiumAccent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .staggeredFadeSlide(index)
            .padding(horizontal = Theme.spacing.md, vertical = Theme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.sm),
    ) {
        Box(
            modifier = Modifier
                .size(Theme.dimensions.touchTargetSmall)
                .background(accent.copy(alpha = 0.12f), RoundedCornerShape(Theme.shapes.medium)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(Theme.dimensions.iconSizeMedium),
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Theme.spacing.xxxs),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (unlocked) {
            Icon(
                Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = Theme.colors.success,
                modifier = Modifier.size(Theme.dimensions.iconSizeMedium),
            )
        }
    }
    if (showDivider) {
        HorizontalDivider(
            modifier = Modifier.padding(
                start = Theme.spacing.md + Theme.dimensions.touchTargetSmall + Theme.spacing.sm,
            ),
            thickness = Theme.dimensions.hairlineThickness,
            color = MaterialTheme.colorScheme.outlineVariant,
        )
    }
}

/** What stays free, stated up front so nobody subscribes for something they already have. */
@Composable
fun FreePlanNote(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                RoundedCornerShape(Theme.shapes.large),
            )
            .padding(Theme.spacing.md),
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.sm),
    ) {
        Icon(
            Icons.Outlined.CheckCircle,
            contentDescription = null,
            tint = Theme.colors.success,
            modifier = Modifier.size(Theme.dimensions.iconSizeMedium),
        )
        Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.xxxs)) {
            Text(
                text = stringResource(Res.string.paywall_free_forever),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(Res.string.paywall_free_forever_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

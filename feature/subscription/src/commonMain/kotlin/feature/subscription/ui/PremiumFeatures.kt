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
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import components.animation.staggeredFadeSlide
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.paywall_feature_ai_credits
import lexicon.resources.generated.resources.paywall_feature_ai_credits_desc
import lexicon.resources.generated.resources.paywall_feature_ai_credits_generic
import lexicon.resources.generated.resources.paywall_free_forever
import lexicon.resources.generated.resources.paywall_free_forever_desc
import org.jetbrains.compose.resources.stringResource
import theme.AppColors
import theme.Theme

/**
 * What premium adds: the monthly AI credit allowance (server `app.credits`). Every other feature is
 * free for everyone. With [unlocked] the row carries a check, for subscribers reviewing their plan.
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
            )
        }
    }
}

@Composable
private fun PremiumFeatureRow(
    icon: ImageVector,
    title: String,
    description: String,
    unlocked: Boolean,
) {
    val accent = AppColors.subscriptionPremiumAccent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .staggeredFadeSlide(0)
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

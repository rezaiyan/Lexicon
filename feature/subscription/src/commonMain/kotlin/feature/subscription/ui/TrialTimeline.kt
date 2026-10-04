package feature.subscription.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import core.getPlatformName
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.paywall_store_app_store
import lexicon.resources.generated.resources.paywall_store_generic
import lexicon.resources.generated.resources.paywall_store_google_play
import lexicon.resources.generated.resources.paywall_timeline_cancel
import lexicon.resources.generated.resources.paywall_timeline_cancel_desc
import lexicon.resources.generated.resources.paywall_timeline_charge
import lexicon.resources.generated.resources.paywall_timeline_charge_desc
import lexicon.resources.generated.resources.paywall_timeline_title
import lexicon.resources.generated.resources.paywall_timeline_today
import lexicon.resources.generated.resources.paywall_timeline_today_desc
import org.jetbrains.compose.resources.stringResource
import theme.AppColors
import theme.Theme

/**
 * Shows exactly what happens during a free trial, with real dates and the real price, so
 * nobody is surprised by the first charge.
 */
@Composable
fun TrialTimeline(
    lastFreeDate: String,
    chargeDate: String,
    price: String,
    modifier: Modifier = Modifier,
) {
    val store = storeName()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(Theme.shapes.large))
            .border(
                Theme.dimensions.borderWidth,
                MaterialTheme.colorScheme.outlineVariant,
                RoundedCornerShape(Theme.shapes.large),
            )
            .padding(Theme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.sm),
    ) {
        Text(
            text = stringResource(Res.string.paywall_timeline_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        TimelineStep(
            icon = Icons.Outlined.LockOpen,
            color = AppColors.subscriptionPremiumAccent,
            title = stringResource(Res.string.paywall_timeline_today),
            description = stringResource(Res.string.paywall_timeline_today_desc),
            showConnector = true,
        )
        TimelineStep(
            icon = Icons.Outlined.NotificationsNone,
            color = AppColors.subscriptionRecommended,
            title = stringResource(Res.string.paywall_timeline_cancel, lastFreeDate),
            description = stringResource(Res.string.paywall_timeline_cancel_desc, store),
            showConnector = true,
        )
        TimelineStep(
            icon = Icons.Outlined.CreditCard,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            title = stringResource(Res.string.paywall_timeline_charge, chargeDate),
            description = stringResource(Res.string.paywall_timeline_charge_desc, price),
            showConnector = false,
        )
    }
}

@Composable
private fun TimelineStep(
    icon: ImageVector,
    color: Color,
    title: String,
    description: String,
    showConnector: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.sm),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxHeight()) {
            Box(
                modifier = Modifier
                    .size(Theme.dimensions.iconSizeXLarge)
                    .background(color.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(Theme.dimensions.iconSizeSmall),
                )
            }
            if (showConnector) {
                Box(
                    modifier = Modifier
                        .padding(top = Theme.spacing.xxs)
                        .width(Theme.dimensions.dividerThickness * 2)
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (showConnector) Theme.spacing.xs else Theme.spacing.none),
            verticalArrangement = Arrangement.spacedBy(Theme.spacing.xxxs),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Where the user manages billing on this platform, phrased to fit "in …" sentences. */
@Composable
internal fun storeName(): String = when (getPlatformName()) {
    "Android" -> stringResource(Res.string.paywall_store_google_play)
    "iOS" -> stringResource(Res.string.paywall_store_app_store)
    else -> stringResource(Res.string.paywall_store_generic)
}

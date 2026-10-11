package feature.support.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SupportAgent
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import domain.support.model.SupportDeliveryStatus
import domain.support.model.SupportSender
import feature.support.model.SupportDay
import feature.support.model.SupportTimelineItem
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.support_not_sent_retry
import lexicon.resources.generated.resources.support_sending
import lexicon.resources.generated.resources.support_sent
import lexicon.resources.generated.resources.support_team_name
import lexicon.resources.generated.resources.support_today
import lexicon.resources.generated.resources.support_yesterday
import org.jetbrains.compose.resources.stringResource
import theme.Theme

/** Centered pill separating days in the conversation. */
@Composable
internal fun DayHeader(day: SupportDay, modifier: Modifier = Modifier) {
    val label = when (day) {
        SupportDay.Today -> stringResource(Res.string.support_today)
        SupportDay.Yesterday -> stringResource(Res.string.support_yesterday)
        is SupportDay.Date -> day.text
    }
    Box(modifier = modifier.fillMaxWidth().padding(vertical = Theme.spacing.sm), contentAlignment = Alignment.Center) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .clip(RoundedCornerShape(Theme.shapes.pill))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(horizontal = Theme.spacing.sm, vertical = Theme.spacing.xxs),
        )
    }
}

/**
 * One message. Bubbles of the same sender sent close together are stacked tightly with their
 * joining corners squared off; the admin avatar and the time sit on the last bubble of a run.
 */
@Composable
internal fun MessageRow(
    bubble: SupportTimelineItem.Bubble,
    onRetry: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val message = bubble.message
    val fromUser = message.sender == SupportSender.USER
    val failed = message.status == SupportDeliveryStatus.FAILED

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = if (bubble.isFirstInGroup) Theme.spacing.sm else Theme.spacing.xxxs)
            // The gap on the far side keeps bubbles from spanning the whole row.
            .padding(start = if (fromUser) Theme.spacing.xxxl else Theme.spacing.none),
        horizontalArrangement = if (fromUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom,
    ) {
        if (!fromUser) {
            if (bubble.isLastInGroup) AdminAvatar() else Spacer(Modifier.width(Theme.dimensions.iconSizeLarge))
            Spacer(Modifier.width(Theme.spacing.xs))
        }
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .padding(end = if (fromUser) Theme.spacing.none else Theme.spacing.xxxl),
            horizontalAlignment = if (fromUser) Alignment.End else Alignment.Start,
        ) {
            if (!fromUser && bubble.isFirstInGroup) {
                Text(
                    text = stringResource(Res.string.support_team_name),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = Theme.spacing.sm, bottom = Theme.spacing.xxs),
                )
            }
            BubbleText(
                body = message.body,
                fromUser = fromUser,
                failed = failed,
                isFirstInGroup = bubble.isFirstInGroup,
                isLastInGroup = bubble.isLastInGroup,
                modifier = if (failed) {
                    Modifier.clickable(role = Role.Button) { onRetry(message.id) }
                } else {
                    Modifier
                },
            )
            when {
                failed -> FailedLabel(onRetry = { onRetry(message.id) })
                message.status == SupportDeliveryStatus.SENDING ->
                    MetaLabel(bubble.time, Icons.Rounded.Schedule, stringResource(Res.string.support_sending))
                bubble.isLastInGroup && fromUser ->
                    MetaLabel(bubble.time, Icons.Rounded.Done, stringResource(Res.string.support_sent))
                bubble.isLastInGroup -> MetaLabel(bubble.time)
            }
        }
    }
}

@Composable
private fun BubbleText(
    body: String,
    fromUser: Boolean,
    failed: Boolean,
    isFirstInGroup: Boolean,
    isLastInGroup: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val round = Theme.shapes.large
    val joined = Theme.shapes.extraSmall
    // Corners on the sender's side are squared where the bubble joins its neighbours.
    val shape = if (fromUser) {
        RoundedCornerShape(
            topStart = round,
            topEnd = if (isFirstInGroup) round else joined,
            bottomEnd = if (isLastInGroup) round else joined,
            bottomStart = round,
        )
    } else {
        RoundedCornerShape(
            topStart = if (isFirstInGroup) round else joined,
            topEnd = round,
            bottomEnd = round,
            bottomStart = if (isLastInGroup) round else joined,
        )
    }
    val container = when {
        failed -> colors.errorContainer
        fromUser -> colors.primary
        else -> colors.surfaceContainerHigh
    }
    val content = when {
        failed -> colors.onErrorContainer
        fromUser -> colors.onPrimary
        else -> colors.onSurface
    }
    Text(
        text = body,
        style = MaterialTheme.typography.bodyLarge,
        color = content,
        modifier = Modifier
            .clip(shape)
            .then(modifier)
            .background(container)
            .padding(
                horizontal = Theme.spacing.sm + Theme.spacing.xxs,
                vertical = Theme.spacing.xs + Theme.spacing.xxxs,
            ),
    )
}

@Composable
private fun AdminAvatar() {
    Box(
        modifier = Modifier
            .size(Theme.dimensions.iconSizeLarge)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.SupportAgent,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(Theme.dimensions.iconSizeSmall),
        )
    }
}

/** Time under the last bubble of a run, with the delivery icon for the user's own messages. */
@Composable
private fun MetaLabel(time: String, icon: ImageVector? = null, iconDescription: String? = null) {
    Row(
        modifier = Modifier.padding(top = Theme.spacing.xxs, start = Theme.spacing.xxs, end = Theme.spacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xxs),
    ) {
        Text(
            text = time,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = iconDescription,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(Theme.dimensions.iconSizeSmall),
            )
        }
    }
}

@Composable
private fun FailedLabel(onRetry: () -> Unit) {
    Row(
        modifier = Modifier
            .padding(top = Theme.spacing.xxs)
            .clip(RoundedCornerShape(Theme.shapes.pill))
            .clickable(role = Role.Button, onClick = onRetry)
            .padding(horizontal = Theme.spacing.xxs, vertical = Theme.spacing.xxxs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xxs),
    ) {
        Icon(
            imageVector = Icons.Rounded.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(Theme.dimensions.iconSizeSmall),
        )
        Text(
            text = stringResource(Res.string.support_not_sent_retry),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

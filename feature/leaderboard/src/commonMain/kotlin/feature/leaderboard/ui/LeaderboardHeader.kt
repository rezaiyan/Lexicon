package feature.leaderboard.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import components.AccentIconBadge
import feature.leaderboard.model.LeaderboardEntryUiModel
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.leaderboard_legend_best_streak
import lexicon.resources.generated.resources.leaderboard_legend_mastered
import lexicon.resources.generated.resources.leaderboard_legend_streak
import lexicon.resources.generated.resources.leaderboard_next_up
import lexicon.resources.generated.resources.leaderboard_show_me
import lexicon.resources.generated.resources.leaderboard_unranked_subtitle
import lexicon.resources.generated.resources.leaderboard_unranked_title
import lexicon.resources.generated.resources.leaderboard_you_lead
import lexicon.resources.generated.resources.leaderboard_your_ranking
import org.jetbrains.compose.resources.stringResource
import theme.Theme

private val HeroAvatarSize = 52.dp
private val PodiumWinnerAvatarSize = 68.dp
private val PodiumAvatarSize = 54.dp
private val PodiumWinnerPedestalHeight = 64.dp
private val PodiumRunnerUpPedestalHeight = 46.dp
private val PodiumThirdPedestalHeight = 32.dp

// region Your standing

/** Shell shared by the ranked and unranked versions of the "Your standing" card. */
@Composable
private fun StandingCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Theme.shapes.large),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            Theme.dimensions.borderWidth,
            MaterialTheme.colorScheme.primary.copy(alpha = Theme.opacity.dimming)
        ),
        shadowElevation = Theme.elevation.low
    ) {
        Column(
            modifier = Modifier
                .background(Theme.gradients.primaryWash)
                .padding(Theme.spacing.cardPadding)
        ) {
            content()
        }
    }
}

/**
 * The user's own standing, first thing on the screen: rank, their three stats with labels,
 * and who to overtake next. [onShowMe] scrolls to the user's row when it is in the list.
 */
@Composable
internal fun YourStandingCard(
    entry: LeaderboardEntryUiModel,
    nextUp: LeaderboardEntryUiModel?,
    onShowMe: (() -> Unit)?
) {
    StandingCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LeaderboardAvatar(
                profileImageUrl = entry.profileImageUrl,
                displayName = entry.displayName,
                size = HeroAvatarSize,
                ringColor = medalColorFor(entry.rank) ?: MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(Theme.spacing.sm))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.leaderboard_your_ranking),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.semantics { heading() }
                )
                Text(
                    text = entry.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(Theme.spacing.sm))
            Text(
                text = "#${entry.rank}",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(Theme.spacing.md))

        Row(horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xs)) {
            StatTile(
                icon = Icons.Default.LocalFireDepartment,
                tint = MaterialTheme.colorScheme.error,
                value = entry.currentStreak,
                label = stringResource(Res.string.leaderboard_legend_streak),
                modifier = Modifier.weight(1f)
            )
            StatTile(
                icon = Icons.Default.Star,
                tint = MaterialTheme.colorScheme.tertiary,
                value = entry.longestStreak,
                label = stringResource(Res.string.leaderboard_legend_best_streak),
                modifier = Modifier.weight(1f)
            )
            StatTile(
                icon = Icons.Default.School,
                tint = MaterialTheme.colorScheme.primary,
                value = entry.masteredWords,
                label = stringResource(Res.string.leaderboard_legend_mastered),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(Theme.spacing.sm))

        StandingStatusRow(rank = entry.rank, nextUp = nextUp, onShowMe = onShowMe)
    }
}

@Composable
private fun StandingStatusRow(
    rank: Int,
    nextUp: LeaderboardEntryUiModel?,
    onShowMe: (() -> Unit)?
) {
    val message = when {
        rank == 1 -> stringResource(Res.string.leaderboard_you_lead)
        nextUp != null -> stringResource(Res.string.leaderboard_next_up, nextUp.displayName, nextUp.rank)
        else -> null
    }
    if (message == null && onShowMe == null) return

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (message != null) {
            Icon(
                imageVector = if (rank == 1) Icons.Rounded.EmojiEvents else Icons.Rounded.TrendingUp,
                contentDescription = null,
                modifier = Modifier.size(Theme.dimensions.iconSizeSmall),
                tint = if (rank == 1) Gold else MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.width(Theme.spacing.xs))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
        if (onShowMe != null) {
            TextButton(onClick = onShowMe) {
                Text(
                    text = stringResource(Res.string.leaderboard_show_me),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun StatTile(
    icon: ImageVector,
    tint: Color,
    value: Int,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(Theme.shapes.medium))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = Theme.opacity.muted))
            .padding(horizontal = Theme.spacing.xs, vertical = Theme.spacing.sm)
            .clearAndSetSemantics { contentDescription = "$value $label" },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(Theme.dimensions.iconSizeSmall),
            tint = tint
        )
        Spacer(modifier = Modifier.height(Theme.spacing.xxs))
        Text(
            text = "$value",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Shown in place of [YourStandingCard] when the user has no rank yet. */
@Composable
internal fun UnrankedStandingCard() {
    StandingCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AccentIconBadge(
                icon = Icons.Rounded.EmojiEvents,
                accent = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(Theme.spacing.sm))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.leaderboard_unranked_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(Theme.spacing.xxxs))
                Text(
                    text = stringResource(Res.string.leaderboard_unranked_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// endregion

// region Podium

/** Top three, laid out 2nd / 1st / 3rd with the winner raised in the middle. */
@Composable
internal fun LeaderboardPodium(entries: List<LeaderboardEntryUiModel>) {
    val first = entries.getOrNull(0) ?: return
    val second = entries.getOrNull(1) ?: return
    val third = entries.getOrNull(2) ?: return

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Theme.shapes.large),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = Theme.elevation.low
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = Theme.spacing.sm, end = Theme.spacing.sm, top = Theme.spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xs),
            verticalAlignment = Alignment.Bottom
        ) {
            PodiumSpot(second, PodiumAvatarSize, PodiumRunnerUpPedestalHeight, Modifier.weight(1f))
            PodiumSpot(first, PodiumWinnerAvatarSize, PodiumWinnerPedestalHeight, Modifier.weight(1f))
            PodiumSpot(third, PodiumAvatarSize, PodiumThirdPedestalHeight, Modifier.weight(1f))
        }
    }
}

@Composable
private fun PodiumSpot(
    entry: LeaderboardEntryUiModel,
    avatarSize: Dp,
    pedestalHeight: Dp,
    modifier: Modifier = Modifier
) {
    val medalColor = medalColorFor(entry.rank) ?: MaterialTheme.colorScheme.outlineVariant
    val label = entry.accessibilityLabel()

    Column(
        modifier = modifier.clearAndSetSemantics { contentDescription = label },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (entry.rank == 1) {
            Icon(
                imageVector = Icons.Rounded.EmojiEvents,
                contentDescription = null,
                modifier = Modifier.size(Theme.dimensions.iconSize),
                tint = Gold
            )
            Spacer(modifier = Modifier.height(Theme.spacing.xxs))
        }
        LeaderboardAvatar(
            profileImageUrl = entry.profileImageUrl,
            displayName = entry.displayName,
            size = avatarSize,
            ringColor = medalColor
        )
        Spacer(modifier = Modifier.height(Theme.spacing.xs))
        Text(
            text = entry.displayName,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = if (entry.isCurrentUser) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (entry.isCurrentUser) {
            Spacer(modifier = Modifier.height(Theme.spacing.xxxs))
            YouPill()
        }
        Spacer(modifier = Modifier.height(Theme.spacing.xxs))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xxs)
        ) {
            MasteredWordsCount(count = entry.masteredWords)
            StreakChip(
                icon = Icons.Default.LocalFireDepartment,
                value = "${entry.currentStreak}",
                tintColor = MaterialTheme.colorScheme.error
            )
        }
        Spacer(modifier = Modifier.height(Theme.spacing.xs))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(pedestalHeight)
                .clip(
                    RoundedCornerShape(
                        topStart = Theme.shapes.medium,
                        topEnd = Theme.shapes.medium
                    )
                )
                .background(
                    Brush.verticalGradient(
                        listOf(medalColor.copy(alpha = 0.5f), medalColor.copy(alpha = 0.1f))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${entry.rank}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// endregion

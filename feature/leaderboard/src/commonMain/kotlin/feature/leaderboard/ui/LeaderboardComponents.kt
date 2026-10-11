package feature.leaderboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import feature.leaderboard.model.LeaderboardEntryUiModel
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.leaderboard_entry_a11y
import lexicon.resources.generated.resources.leaderboard_jump_to_me
import lexicon.resources.generated.resources.leaderboard_legend_best_streak
import lexicon.resources.generated.resources.leaderboard_legend_mastered
import lexicon.resources.generated.resources.leaderboard_legend_streak
import lexicon.resources.generated.resources.leaderboard_refresh_failed
import lexicon.resources.generated.resources.leaderboard_unranked_subtitle
import lexicon.resources.generated.resources.leaderboard_unranked_title
import lexicon.resources.generated.resources.leaderboard_you
import lexicon.resources.generated.resources.leaderboard_your_ranking
import lexicon.resources.generated.resources.retry
import org.jetbrains.compose.resources.stringResource
import theme.AppColors
import theme.Theme

internal val Gold = AppColors.leaderboardGold
internal val Silver = AppColors.leaderboardSilver
internal val Bronze = AppColors.leaderboardBronze

private val RankBadgeSize = 36.dp
private val StatIconSize = 12.dp
private val PodiumWinnerAvatarSize = 64.dp
private val PodiumAvatarSize = 52.dp
private val PodiumWinnerPedestalHeight = 72.dp
private val PodiumRunnerUpPedestalHeight = 52.dp
private val PodiumThirdPedestalHeight = 36.dp

private fun medalColorFor(rank: Int): Color? = when (rank) {
    1 -> Gold
    2 -> Silver
    3 -> Bronze
    else -> null
}

/** One spoken sentence per entry, so screen readers don't read loose numbers and icons. */
@Composable
private fun LeaderboardEntryUiModel.accessibilityLabel(): String = stringResource(
    Res.string.leaderboard_entry_a11y,
    rank,
    displayName,
    currentStreak,
    longestStreak,
    masteredWords,
)

// region Podium

/** Top three entries, laid out 2nd / 1st / 3rd with the winner raised in the middle. */
@Composable
internal fun LeaderboardPodium(entries: List<LeaderboardEntryUiModel>) {
    val first = entries.getOrNull(0) ?: return
    val second = entries.getOrNull(1) ?: return
    val third = entries.getOrNull(2) ?: return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Theme.spacing.sm, bottom = Theme.spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xs),
        verticalAlignment = Alignment.Bottom
    ) {
        PodiumSpot(second, PodiumAvatarSize, PodiumRunnerUpPedestalHeight, Modifier.weight(1f))
        PodiumSpot(first, PodiumWinnerAvatarSize, PodiumWinnerPedestalHeight, Modifier.weight(1f))
        PodiumSpot(third, PodiumAvatarSize, PodiumThirdPedestalHeight, Modifier.weight(1f))
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
        MasteredWordsCount(count = entry.masteredWords)
        Spacer(modifier = Modifier.height(Theme.spacing.xxxs))
        StreakChip(
            icon = Icons.Default.LocalFireDepartment,
            value = "${entry.currentStreak}",
            tintColor = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(Theme.spacing.xs))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(pedestalHeight)
                .clip(
                    RoundedCornerShape(
                        topStart = Theme.dimensions.cardCornerRadius,
                        topEnd = Theme.dimensions.cardCornerRadius
                    )
                )
                .background(
                    Brush.verticalGradient(
                        listOf(medalColor.copy(alpha = 0.45f), medalColor.copy(alpha = 0.08f))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${entry.rank}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// endregion

// region Legend & banners

/** Explains the icons used on every row, so the numbers are not a guessing game. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun StatsLegend() {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Theme.spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.md, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.xxs)
    ) {
        LegendItem(
            icon = Icons.Default.LocalFireDepartment,
            tint = MaterialTheme.colorScheme.error,
            label = stringResource(Res.string.leaderboard_legend_streak)
        )
        LegendItem(
            icon = Icons.Default.Star,
            tint = MaterialTheme.colorScheme.tertiary,
            label = stringResource(Res.string.leaderboard_legend_best_streak)
        )
        LegendItem(
            icon = Icons.Default.School,
            tint = MaterialTheme.colorScheme.primary,
            label = stringResource(Res.string.leaderboard_legend_mastered)
        )
    }
}

@Composable
private fun LegendItem(icon: ImageVector, tint: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xxs)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(StatIconSize),
            tint = tint.copy(alpha = 0.8f)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Shown above stale content when a refresh fails, instead of wiping the list. */
@Composable
internal fun RefreshFailedBanner(onRetry: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Theme.dimensions.cardCornerRadius),
        color = MaterialTheme.colorScheme.errorContainer
    ) {
        Row(
            modifier = Modifier.padding(start = Theme.spacing.md, end = Theme.spacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(Res.string.leaderboard_refresh_failed),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onRetry) {
                Text(
                    text = stringResource(Res.string.retry),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// endregion

// region Rows

@Composable
internal fun LeaderboardEntryCard(entry: LeaderboardEntryUiModel) {
    val cardShape = RoundedCornerShape(Theme.dimensions.cardCornerRadius)
    val label = entry.accessibilityLabel()
    val borderModifier = if (entry.isCurrentUser) {
        Modifier.border(
            width = Theme.dimensions.borderWidth,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
            shape = cardShape
        )
    } else {
        Modifier
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(borderModifier)
            .clearAndSetSemantics { contentDescription = label },
        shape = cardShape,
        color = if (entry.isCurrentUser) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerLowest
        },
        tonalElevation = Theme.elevation.none
    ) {
        EntryRow(
            entry = entry,
            modifier = Modifier.padding(horizontal = Theme.spacing.md, vertical = Theme.spacing.sm)
        )
    }
}

@Composable
private fun EntryRow(entry: LeaderboardEntryUiModel, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RankBadge(rank = entry.rank, medalColor = medalColorFor(entry.rank))

        Spacer(modifier = Modifier.width(Theme.spacing.sm))

        LeaderboardAvatar(
            profileImageUrl = entry.profileImageUrl,
            displayName = entry.displayName
        )

        Spacer(modifier = Modifier.width(Theme.spacing.sm))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xxs)
            ) {
                Text(
                    text = entry.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (entry.isCurrentUser) FontWeight.Bold else FontWeight.Medium,
                    color = if (entry.isCurrentUser) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (entry.isCurrentUser) {
                    YouPill()
                }
            }
            Spacer(modifier = Modifier.height(Theme.spacing.xxxs))
            StreakInfo(entry = entry)
        }

        Spacer(modifier = Modifier.width(Theme.spacing.sm))

        MasteredWordsCount(count = entry.masteredWords)
    }
}

@Composable
private fun LeaderboardAvatar(
    profileImageUrl: String?,
    displayName: String,
    modifier: Modifier = Modifier,
    size: Dp = Theme.dimensions.iconSizeXLarge,
    ringColor: Color? = null
) {
    val ringModifier = if (ringColor != null) {
        Modifier.border(Theme.dimensions.borderWidthThick, ringColor, CircleShape)
    } else {
        Modifier
    }
    Box(
        modifier = modifier
            .size(size)
            .then(ringModifier)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
        contentAlignment = Alignment.Center
    ) {
        if (profileImageUrl != null) {
            AsyncImage(
                model = profileImageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize().clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            val initial = displayName.firstOrNull()?.uppercaseChar()
            if (initial != null && initial.isLetter()) {
                Text(
                    text = initial.toString(),
                    style = if (size > Theme.dimensions.iconSizeXLarge) {
                        MaterialTheme.typography.titleLarge
                    } else {
                        MaterialTheme.typography.labelMedium
                    },
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(size / 2),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun RankBadge(rank: Int, medalColor: Color?) {
    Box(
        modifier = Modifier
            .size(RankBadgeSize)
            .clip(RoundedCornerShape(Theme.shapes.small))
            .background(
                medalColor?.copy(alpha = 0.18f)
                    ?: MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$rank",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun YouPill() {
    Text(
        text = stringResource(Res.string.leaderboard_you),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onPrimary,
        modifier = Modifier
            .clip(RoundedCornerShape(Theme.shapes.pill))
            .background(MaterialTheme.colorScheme.primary)
            .padding(horizontal = Theme.spacing.xs, vertical = Theme.spacing.xxxs)
    )
}

@Composable
private fun StreakInfo(entry: LeaderboardEntryUiModel) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xxs)
    ) {
        StreakChip(
            icon = Icons.Default.LocalFireDepartment,
            value = "${entry.currentStreak}",
            tintColor = MaterialTheme.colorScheme.error
        )

        // Best streak only adds information when it differs from the current one.
        if (entry.longestStreak > entry.currentStreak) {
            StreakChip(
                icon = Icons.Default.Star,
                value = "${entry.longestStreak}",
                tintColor = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}

@Composable
private fun StreakChip(
    icon: ImageVector,
    value: String,
    tintColor: Color
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(Theme.shapes.pill))
            .background(tintColor.copy(alpha = 0.08f))
            .padding(horizontal = Theme.spacing.xxs, vertical = Theme.spacing.xxxs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xxxs)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(StatIconSize),
            tint = tintColor.copy(alpha = 0.8f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = tintColor.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun MasteredWordsCount(count: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xxs)
    ) {
        Icon(
            imageVector = Icons.Default.School,
            contentDescription = null,
            modifier = Modifier.size(Theme.dimensions.iconSizeSmall),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
        )
        Text(
            text = "$count",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

// endregion

// region Footers

@Composable
private fun FooterContainer(
    onClick: (() -> Unit)?,
    content: @Composable () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val cardCornerRadius = Theme.dimensions.cardCornerRadius
    val borderWidth = Theme.dimensions.borderWidth
    val cardShape = RoundedCornerShape(
        topStart = cardCornerRadius,
        topEnd = cardCornerRadius
    )
    val clickModifier = if (onClick != null) {
        Modifier.clickable(role = Role.Button, onClick = onClick)
    } else {
        Modifier
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .then(clickModifier)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
            .drawBehind {
                drawRoundRect(
                    brush = Brush.linearGradient(
                        listOf(
                            primaryColor.copy(alpha = 0.22f),
                            primaryColor.copy(alpha = 0.06f)
                        )
                    ),
                    cornerRadius = CornerRadius(cardCornerRadius.toPx()),
                    style = Stroke(width = borderWidth.toPx())
                )
            }
            .padding(
                start = Theme.spacing.md,
                end = Theme.spacing.md,
                top = Theme.spacing.sm,
                bottom = Theme.spacing.md
            )
    ) {
        content()
    }
}

/**
 * Pinned card with the current user's ranking. Shown while their own row is off-screen;
 * tapping it scrolls to that row when [onJumpToEntry] is provided.
 */
@Composable
internal fun UserRankingFooter(
    entry: LeaderboardEntryUiModel,
    onJumpToEntry: (() -> Unit)?
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val label = entry.accessibilityLabel()
    val jumpLabel = stringResource(Res.string.leaderboard_jump_to_me)

    FooterContainer(onClick = onJumpToEntry) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(Res.string.leaderboard_your_ranking),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = primaryColor,
                letterSpacing = 0.3.sp,
                modifier = Modifier.weight(1f)
            )
            if (onJumpToEntry != null) {
                Text(
                    text = jumpLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = null,
                    modifier = Modifier.size(Theme.dimensions.iconSizeSmall),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(Theme.spacing.xs))
        EntryRow(
            entry = entry,
            modifier = Modifier.clearAndSetSemantics {
                contentDescription = if (onJumpToEntry != null) "$label. $jumpLabel" else label
            }
        )
    }
}

/** Shown when the current user has no rank yet, with a nudge on how to get one. */
@Composable
internal fun UnrankedFooter() {
    FooterContainer(onClick = null) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(RankBadgeSize)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.EmojiEvents,
                    contentDescription = null,
                    modifier = Modifier.size(Theme.dimensions.iconSizeMedium),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(Theme.spacing.sm))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.leaderboard_unranked_title),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
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

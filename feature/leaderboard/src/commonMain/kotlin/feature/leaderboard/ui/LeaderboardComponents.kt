package feature.leaderboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import components.SectionHeader
import feature.leaderboard.model.LeaderboardEntryUiModel
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.leaderboard_entry_a11y
import lexicon.resources.generated.resources.leaderboard_jump_to_me
import lexicon.resources.generated.resources.leaderboard_legend_best_streak
import lexicon.resources.generated.resources.leaderboard_legend_mastered
import lexicon.resources.generated.resources.leaderboard_legend_streak
import lexicon.resources.generated.resources.leaderboard_rankings
import lexicon.resources.generated.resources.leaderboard_refresh_failed
import lexicon.resources.generated.resources.leaderboard_you
import lexicon.resources.generated.resources.retry
import org.jetbrains.compose.resources.stringResource
import theme.AppColors
import theme.Theme

internal val Gold = AppColors.leaderboardGold
internal val Silver = AppColors.leaderboardSilver
internal val Bronze = AppColors.leaderboardBronze

private val RankColumnWidth = 32.dp
private val RowAvatarSize = 40.dp
private val FloatingAvatarSize = 24.dp
private val StatIconSize = 12.dp

internal fun medalColorFor(rank: Int): Color? = when (rank) {
    1 -> Gold
    2 -> Silver
    3 -> Bronze
    else -> null
}

/** One spoken sentence per entry, so screen readers don't read loose numbers and icons. */
@Composable
internal fun LeaderboardEntryUiModel.accessibilityLabel(): String = stringResource(
    Res.string.leaderboard_entry_a11y,
    rank,
    displayName,
    currentStreak,
    longestStreak,
    masteredWords,
)

// region Rankings section

/** "Rankings" title with the number of ranked people, plus a legend for the row icons. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun RankingsHeader(count: Int) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = Theme.spacing.sm)) {
        SectionHeader(
            title = stringResource(Res.string.leaderboard_rankings),
            count = count
        )
        Spacer(modifier = Modifier.height(Theme.spacing.xs))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Theme.spacing.md),
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
            tint = tint
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * One row of the rankings list. Consecutive rows read as a single grouped card:
 * only the first and last round their outer corners, and rows are split by hairlines.
 */
@Composable
internal fun RankingRow(
    entry: LeaderboardEntryUiModel,
    isFirst: Boolean,
    isLast: Boolean
) {
    val corner = Theme.shapes.large
    val shape = RoundedCornerShape(
        topStart = if (isFirst) corner else 0.dp,
        topEnd = if (isFirst) corner else 0.dp,
        bottomStart = if (isLast) corner else 0.dp,
        bottomEnd = if (isLast) corner else 0.dp
    )
    val label = entry.accessibilityLabel()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = label },
        shape = shape,
        color = if (entry.isCurrentUser) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = Theme.opacity.dimming)
        } else {
            MaterialTheme.colorScheme.surface
        }
    ) {
        Column {
            EntryRow(
                entry = entry,
                modifier = Modifier.padding(
                    horizontal = Theme.spacing.md,
                    vertical = Theme.spacing.sm
                )
            )
            if (!isLast) {
                HorizontalDivider(
                    modifier = Modifier.padding(
                        start = Theme.spacing.md + RankColumnWidth + Theme.spacing.xs
                    ),
                    thickness = Theme.dimensions.hairlineThickness,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }
        }
    }
}

@Composable
private fun EntryRow(entry: LeaderboardEntryUiModel, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RankLabel(rank = entry.rank)

        Spacer(modifier = Modifier.width(Theme.spacing.xs))

        LeaderboardAvatar(
            profileImageUrl = entry.profileImageUrl,
            displayName = entry.displayName,
            size = RowAvatarSize,
            ringColor = medalColorFor(entry.rank)
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
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (entry.isCurrentUser) {
                    YouPill()
                }
            }
            Spacer(modifier = Modifier.height(Theme.spacing.xxs))
            StreakInfo(entry = entry)
        }

        Spacer(modifier = Modifier.width(Theme.spacing.sm))

        MasteredWordsCount(count = entry.masteredWords)
    }
}

@Composable
private fun RankLabel(rank: Int) {
    Box(
        modifier = Modifier.width(RankColumnWidth),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$rank",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

// endregion

// region Shared atoms

@Composable
internal fun LeaderboardAvatar(
    profileImageUrl: String?,
    displayName: String,
    size: Dp,
    modifier: Modifier = Modifier,
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
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = Theme.opacity.dimming)),
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
                    style = if (size > RowAvatarSize) {
                        MaterialTheme.typography.titleLarge
                    } else {
                        MaterialTheme.typography.labelLarge
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
internal fun YouPill() {
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
internal fun StreakChip(
    icon: ImageVector,
    value: String,
    tintColor: Color
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(Theme.shapes.pill))
            .background(tintColor.copy(alpha = Theme.opacity.hover))
            .padding(horizontal = Theme.spacing.xs, vertical = Theme.spacing.xxxs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xxxs)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(StatIconSize),
            tint = tintColor
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = tintColor
        )
    }
}

@Composable
internal fun MasteredWordsCount(count: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xxs)
    ) {
        Icon(
            imageVector = Icons.Default.School,
            contentDescription = null,
            modifier = Modifier.size(Theme.dimensions.iconSizeSmall),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "$count",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// endregion

// region Banners & floating pill

/** Shown above stale content when a refresh fails, instead of wiping the list. */
@Composable
internal fun RefreshFailedBanner(onRetry: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Theme.shapes.medium),
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

/** Compact "#12 · You" pill floating over the list; tapping it scrolls back to the user. */
@Composable
internal fun FloatingYouPill(
    entry: LeaderboardEntryUiModel,
    pointsDown: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val jumpLabel = stringResource(Res.string.leaderboard_jump_to_me)
    val label = entry.accessibilityLabel()

    Surface(
        onClick = onClick,
        modifier = modifier.clearAndSetSemantics { contentDescription = "$label. $jumpLabel" },
        shape = RoundedCornerShape(Theme.shapes.pill),
        color = MaterialTheme.colorScheme.primary,
        shadowElevation = Theme.elevation.high
    ) {
        Row(
            modifier = Modifier.padding(
                start = Theme.spacing.xs,
                end = Theme.spacing.sm,
                top = Theme.spacing.xs,
                bottom = Theme.spacing.xs
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xs)
        ) {
            LeaderboardAvatar(
                profileImageUrl = entry.profileImageUrl,
                displayName = entry.displayName,
                size = FloatingAvatarSize
            )
            Text(
                text = "#${entry.rank}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Text(
                text = stringResource(Res.string.leaderboard_you),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Icon(
                imageVector = if (pointsDown) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                contentDescription = null,
                modifier = Modifier.size(Theme.dimensions.iconSizeMedium),
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

// endregion

package presentation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.leaderboard
import lexicon.resources.generated.resources.leaderboard_current_streak
import lexicon.resources.generated.resources.leaderboard_mastered
import lexicon.resources.generated.resources.leaderboard_your_ranking
import lexicon.resources.generated.resources.refresh
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import presentation.feature.leaderboard.LeaderboardViewModel
import presentation.model.LeaderboardEntryUiModel
import presentation.model.LeaderboardUiData
import core.common.UiState
import components.scaffold.ActionIconConfig
import components.scaffold.LexiconColumn
import theme.Theme

private val Gold = Color(0xFFFFD700)
private val Silver = Color(0xFFC0C0C0)
private val Bronze = Color(0xFFCD7F32)

@Composable
fun LeaderboardScreen(
    onNavigateBack: () -> Unit
) {
    val viewModel = koinViewModel<LeaderboardViewModel>()
    val uiState by viewModel.state()

    LexiconColumn(
        title = stringResource(Res.string.leaderboard),
        showNavigationIcon = true,
        onNavigationClick = onNavigateBack,
        actionIcon1 = ActionIconConfig(
            icon = Icons.Default.Refresh,
            contentDescription = stringResource(Res.string.refresh),
            onClick = viewModel::refresh,
            size = Theme.dimensions.iconSize
        ),
        scrollable = false
    ) {
        when (val state = uiState) {
            is UiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is UiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            is UiState.Loaded -> {
                LeaderboardContent(data = state.value)
            }
        }
    }
}

@Composable
private fun LeaderboardContent(data: LeaderboardUiData) {
    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Theme.spacing.xs)
        ) {
            itemsIndexed(data.entries, key = { _, entry -> entry.rank }) { _, entry ->
                LeaderboardEntryCard(entry = entry)
            }
        }

        if (data.userEntry != null) {
            UserRankingFooter(entry = data.userEntry)
        }
    }
}

@Composable
private fun LeaderboardEntryCard(entry: LeaderboardEntryUiModel) {
    val medalColor = when (entry.rank) {
        1 -> Gold
        2 -> Silver
        3 -> Bronze
        else -> null
    }
    val cardShape = RoundedCornerShape(Theme.dimensions.cardCornerRadius)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = cardShape,
        color = if (entry.isCurrentUser) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerLowest
        },
        tonalElevation = Theme.elevation.none
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Theme.spacing.md, vertical = Theme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank
            RankBadge(rank = entry.rank, medalColor = medalColor)

            Spacer(modifier = Modifier.width(Theme.spacing.sm))

            // Profile avatar
            LeaderboardAvatar(
                profileImageUrl = entry.profileImageUrl,
                displayName = entry.displayName
            )

            Spacer(modifier = Modifier.width(Theme.spacing.sm))

            // Name + streak info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (entry.isCurrentUser) FontWeight.Bold else FontWeight.Medium,
                    color = if (entry.isCurrentUser) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(Theme.spacing.xxxs))
                StreakInfo(entry = entry)
            }

            Spacer(modifier = Modifier.width(Theme.spacing.sm))

            // Mastered words
            MasteredWordsCount(count = entry.masteredWords)
        }
    }
}

@Composable
private fun LeaderboardAvatar(
    profileImageUrl: String?,
    displayName: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(Theme.dimensions.iconSizeXLarge)
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
            val initial = displayName.firstOrNull()?.uppercaseChar()?.toString()
            if (initial != null && initial.first().isLetter()) {
                Text(
                    text = initial,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
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
            .size(36.dp)
            .clip(RoundedCornerShape(Theme.shapes.small))
            .background(
                medalColor?.copy(alpha = 0.12f)
                    ?: MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$rank",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = medalColor ?: MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


@Composable
private fun StreakInfo(entry: LeaderboardEntryUiModel) {
    val streakColor = MaterialTheme.colorScheme.error
    val bestStreakColor = MaterialTheme.colorScheme.tertiary

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xxs)
    ) {
        StreakChip(
            icon = Icons.Default.LocalFireDepartment,
            value = "${entry.currentStreak}",
            tintColor = streakColor,
            contentDescription = stringResource(Res.string.leaderboard_current_streak)
        )

        if (entry.longestStreak > 0 && entry.longestStreak != entry.currentStreak) {
            StreakChip(
                icon = Icons.Default.Star,
                value = "${entry.longestStreak}",
                tintColor = bestStreakColor,
                contentDescription = null
            )
        }
    }
}

@Composable
private fun StreakChip(
    icon: ImageVector,
    value: String,
    tintColor: Color,
    contentDescription: String?
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
            contentDescription = contentDescription,
            modifier = Modifier.size(12.dp),
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
            contentDescription = stringResource(Res.string.leaderboard_mastered),
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

@Composable
private fun UserRankingFooter(entry: LeaderboardEntryUiModel) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val cardCornerRadius = Theme.dimensions.cardCornerRadius
    val borderWidth = Theme.dimensions.borderWidth
    val cardShape = RoundedCornerShape(
        topStart = cardCornerRadius,
        topEnd = cardCornerRadius
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
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
        Text(
            text = stringResource(Res.string.leaderboard_your_ranking),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = primaryColor,
            letterSpacing = 0.3.sp
        )
        Spacer(modifier = Modifier.height(Theme.spacing.xs))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RankBadge(rank = entry.rank, medalColor = when (entry.rank) {
                1 -> Gold; 2 -> Silver; 3 -> Bronze; else -> null
            })
            Spacer(modifier = Modifier.width(Theme.spacing.sm))
            LeaderboardAvatar(
                profileImageUrl = entry.profileImageUrl,
                displayName = entry.displayName
            )
            Spacer(modifier = Modifier.width(Theme.spacing.sm))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(Theme.spacing.xxxs))
                StreakInfo(entry = entry)
            }
            Spacer(modifier = Modifier.width(Theme.spacing.sm))
            MasteredWordsCount(count = entry.masteredWords)
        }
    }
}

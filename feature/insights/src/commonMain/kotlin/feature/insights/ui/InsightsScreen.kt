package feature.insights.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import components.ErrorScreen
import components.LoadingScreen
import components.scaffold.LexiconColumn
import core.common.UiState
import core.common.onError
import core.common.onLoaded
import core.common.onLoading
import domain.analytics.model.AccuracyByLevel
import domain.analytics.model.StudyInsights
import domain.analytics.model.WordDifficulty
import feature.insights.InsightsState
import feature.insights.InsightsTab
import feature.insights.InsightsViewModel
import org.koin.compose.viewmodel.koinViewModel
import theme.Theme

@Composable
fun InsightsScreen(
    onNavigateBack: () -> Unit,
) {
    val viewModel = koinViewModel<InsightsViewModel>()
    val state by viewModel.state()

    InsightsContent(
        state = state,
        onTabSelected = viewModel::selectTab,
        onRefresh = viewModel::refresh,
        onNavigateBack = onNavigateBack,
    )
}

@Composable
private fun InsightsContent(
    state: InsightsState,
    onTabSelected: (InsightsTab) -> Unit,
    onRefresh: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    LexiconColumn(
        title = "Study Insights",
        onNavigationClick = onNavigateBack,
        scrollState = rememberScrollState(),
        scrollable = true,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = Theme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(Theme.spacing.md),
        ) {
            // Tab chips
            Row(
                horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xs),
            ) {
                InsightsTab.entries.forEach { tab ->
                    FilterChip(
                        selected = state.selectedTab == tab,
                        onClick = { onTabSelected(tab) },
                        label = {
                            Text(
                                when (tab) {
                                    InsightsTab.OVERVIEW -> "Overview"
                                    InsightsTab.TRENDS -> "Trends"
                                    InsightsTab.WORDS -> "Words"
                                }
                            )
                        },
                    )
                }
            }

            when (state.selectedTab) {
                InsightsTab.OVERVIEW -> OverviewTab(state)
                InsightsTab.TRENDS -> TrendsTab(state)
                InsightsTab.WORDS -> WordsTab(state)
            }

            Spacer(modifier = Modifier.height(Theme.spacing.xl))
        }
    }
}

@Composable
private fun OverviewTab(state: InsightsState) {
    state.overview
        .onLoading { LoadingScreen(message = "Loading insights...") }
        .onError { msg, _ -> ErrorScreen(message = msg) }
        .onLoaded { insights ->
            OverviewCards(insights)
        }

    state.bestStudyTime.onLoaded { bestTime ->
        if (bestTime != null) {
            StatCard(
                icon = Icons.Default.Schedule,
                title = "Best Study Time",
                value = "${bestTime.hour}:00",
                subtitle = "%.0f%% accuracy".format(bestTime.accuracyPercent),
            )
        }
    }
}

@Composable
private fun OverviewCards(insights: StudyInsights) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.sm),
    ) {
        StatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.MenuBook,
            title = "Cards Reviewed",
            value = insights.totalCardsReviewed.toString(),
        )
        StatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.AutoMirrored.Filled.TrendingUp,
            title = "Accuracy",
            value = "%.1f%%".format(insights.accuracyPercent),
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.sm),
    ) {
        StatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Star,
            title = "Words Mastered",
            value = insights.wordsMasteredCount.toString(),
        )
        StatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.BarChart,
            title = "Days Studied",
            value = insights.daysStudied.toString(),
        )
    }

    val totalTimeMinutes = insights.totalStudyTimeMs / 60_000
    val hours = totalTimeMinutes / 60
    val minutes = totalTimeMinutes % 60
    StatCard(
        icon = Icons.Default.Schedule,
        title = "Total Study Time",
        value = if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m",
        subtitle = "${insights.totalSessions} sessions, ${insights.uniqueWordsReviewed} unique words",
    )
}

@Composable
private fun TrendsTab(state: InsightsState) {
    state.accuracyByLevel
        .onLoading { LoadingScreen(message = "Loading levels...") }
        .onError { msg, _ -> ErrorScreen(message = msg) }
        .onLoaded { levels ->
            if (levels.isEmpty()) {
                Text(
                    "No review data yet. Start studying to see trends!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text("Accuracy by SRS Level", style = MaterialTheme.typography.titleMedium)
                levels.forEach { level ->
                    LevelAccuracyRow(level)
                }
            }
        }

    state.heatmap.onLoaded { days ->
        if (days.isNotEmpty()) {
            Spacer(modifier = Modifier.height(Theme.spacing.sm))
            Text("Activity (last 90 days)", style = MaterialTheme.typography.titleMedium)
            Text(
                "${days.size} active days, ${days.sumOf { it.count }} cards reviewed",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LevelAccuracyRow(level: AccuracyByLevel) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Level ${level.level}", style = MaterialTheme.typography.bodyMedium)
            Text(
                "%.0f%% (%d reviews)".format(level.accuracyPercent, level.totalReviews),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LinearProgressIndicator(
            progress = { (level.accuracyPercent / 100.0).toFloat() },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun WordsTab(state: InsightsState) {
    state.difficultWords
        .onLoading { LoadingScreen(message = "Loading words...") }
        .onError { msg, _ -> ErrorScreen(message = msg) }
        .onLoaded { words ->
            if (words.isEmpty()) {
                Text(
                    "No difficult words found yet. Keep studying!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text("Most Difficult Words", style = MaterialTheme.typography.titleMedium)
                words.forEach { word ->
                    DifficultWordRow(word)
                }
            }
        }
}

@Composable
private fun DifficultWordRow(word: WordDifficulty) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Theme.spacing.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(word.wordText, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(word.wordTranslation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.width(Theme.spacing.sm))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "%.0f%% error".format(word.errorRate * 100),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
                Text(
                    "${word.totalReviews} reviews",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    value: String,
    subtitle: String? = null,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(Theme.spacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(Theme.spacing.xs))
                Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.height(Theme.spacing.xs))
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

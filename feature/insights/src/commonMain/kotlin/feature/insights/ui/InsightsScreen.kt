package feature.insights.ui

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import components.ErrorScreen
import components.LoadingScreen
import components.scaffold.LexiconColumn
import components.scaffold.TopBarColor
import core.common.onError
import core.common.onLoaded
import core.common.onLoading
import domain.analytics.model.AccuracyByLevel
import domain.analytics.model.StudyInsights
import domain.analytics.model.WordDifficulty
import feature.insights.InsightsState
import feature.insights.InsightsTab
import feature.insights.InsightsViewModel
import kotlin.math.roundToInt
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.insights_accuracy
import lexicon.resources.generated.resources.insights_accuracy_by_level
import lexicon.resources.generated.resources.insights_accuracy_format
import lexicon.resources.generated.resources.insights_accuracy_reviews_format
import lexicon.resources.generated.resources.insights_activity_summary
import lexicon.resources.generated.resources.insights_activity_title
import lexicon.resources.generated.resources.insights_best_study_time
import lexicon.resources.generated.resources.insights_cards_reviewed
import lexicon.resources.generated.resources.insights_days_studied
import lexicon.resources.generated.resources.insights_error_rate_format
import lexicon.resources.generated.resources.insights_level_format
import lexicon.resources.generated.resources.insights_loading
import lexicon.resources.generated.resources.insights_loading_levels
import lexicon.resources.generated.resources.insights_loading_words
import lexicon.resources.generated.resources.insights_most_difficult_words
import lexicon.resources.generated.resources.insights_no_difficult_words
import lexicon.resources.generated.resources.insights_no_review_data
import lexicon.resources.generated.resources.insights_reviews_format
import lexicon.resources.generated.resources.insights_sessions_words
import lexicon.resources.generated.resources.insights_tab_overview
import lexicon.resources.generated.resources.insights_tab_trends
import lexicon.resources.generated.resources.insights_tab_words
import lexicon.resources.generated.resources.insights_title
import lexicon.resources.generated.resources.insights_total_study_time
import lexicon.resources.generated.resources.insights_words_mastered
import org.jetbrains.compose.resources.stringResource
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
        onNavigateBack = onNavigateBack,
    )
}

@Composable
private fun InsightsContent(
    state: InsightsState,
    onTabSelected: (InsightsTab) -> Unit,
    onNavigateBack: () -> Unit,
) {
    val availability = state.availability
    val visibleTabs = availability.visibleTabs

    LexiconColumn(
        title = stringResource(Res.string.insights_title),
        showNavigationIcon = true,
        navigationIcon = Icons.Default.Close,
        onNavigationClick = onNavigateBack,
        scrollable = false,
        topBarColor = TopBarColor.Background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Theme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(Theme.spacing.lg),
        ) {
            if (visibleTabs.size > 1) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xs),
                ) {
                    visibleTabs.forEach { tab ->
                        FilterChip(
                            selected = state.selectedTab == tab,
                            onClick = { onTabSelected(tab) },
                            label = {
                                Text(
                                    tabLabel(tab),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (state.selectedTab == tab) {
                                        FontWeight.SemiBold
                                    } else {
                                        FontWeight.Normal
                                    },
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                            shape = RoundedCornerShape(Theme.shapes.pill),
                        )
                    }
                }
            }

            when (state.selectedTab) {
                InsightsTab.OVERVIEW -> OverviewTab(state)
                InsightsTab.TRENDS -> if (availability.hasTrends) TrendsTab(state)
                InsightsTab.WORDS -> if (availability.hasWords) WordsTab(state)
            }

            Spacer(modifier = Modifier.height(Theme.spacing.xl))
        }
    }
}

@Composable
private fun tabLabel(tab: InsightsTab): String = when (tab) {
    InsightsTab.OVERVIEW -> stringResource(Res.string.insights_tab_overview)
    InsightsTab.TRENDS -> stringResource(Res.string.insights_tab_trends)
    InsightsTab.WORDS -> stringResource(Res.string.insights_tab_words)
}

// region Overview

@Composable
private fun OverviewTab(state: InsightsState) {
    state.overview
        .onLoading { LoadingScreen(message = stringResource(Res.string.insights_loading)) }
        .onError { msg, _ -> ErrorScreen(message = msg) }
        .onLoaded { insights ->
            OverviewCards(insights)
        }

    state.bestStudyTime.onLoaded { bestTime ->
        if (bestTime != null) {
            MetricRow(
                icon = Icons.Default.Schedule,
                iconTint = MaterialTheme.colorScheme.tertiary,
                title = stringResource(Res.string.insights_best_study_time),
                value = "${bestTime.hour}:00",
                subtitle = stringResource(Res.string.insights_accuracy_format, bestTime.accuracyPercent.roundToInt()),
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
            icon = Icons.AutoMirrored.Filled.MenuBook,
            iconTint = MaterialTheme.colorScheme.primary,
            label = stringResource(Res.string.insights_cards_reviewed),
            value = insights.totalCardsReviewed.toString(),
        )
        StatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.AutoMirrored.Filled.TrendingUp,
            iconTint = MaterialTheme.colorScheme.secondary,
            label = stringResource(Res.string.insights_accuracy),
            value = "${insights.accuracyPercent.roundToInt()}%",
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.sm),
    ) {
        StatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Star,
            iconTint = MaterialTheme.colorScheme.tertiary,
            label = stringResource(Res.string.insights_words_mastered),
            value = insights.wordsMasteredCount.toString(),
        )
        StatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.BarChart,
            iconTint = MaterialTheme.colorScheme.primary,
            label = stringResource(Res.string.insights_days_studied),
            value = insights.daysStudied.toString(),
        )
    }

    val totalTimeMinutes = insights.totalStudyTimeMs / 60_000
    val hours = totalTimeMinutes / 60
    val minutes = totalTimeMinutes % 60
    MetricRow(
        icon = Icons.Default.Schedule,
        iconTint = MaterialTheme.colorScheme.primary,
        title = stringResource(Res.string.insights_total_study_time),
        value = if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m",
        subtitle = stringResource(
            Res.string.insights_sessions_words,
            insights.totalSessions.toInt(),
            insights.uniqueWordsReviewed.toInt(),
        ),
    )
}

// endregion

// region Trends

@Composable
private fun TrendsTab(state: InsightsState) {
    state.accuracyByLevel
        .onLoading { LoadingScreen(message = stringResource(Res.string.insights_loading_levels)) }
        .onError { msg, _ -> ErrorScreen(message = msg) }
        .onLoaded { levels ->
            if (levels.isEmpty()) {
                Text(
                    stringResource(Res.string.insights_no_review_data),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    stringResource(Res.string.insights_accuracy_by_level),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(Theme.spacing.xs))
                levels.forEachIndexed { index, level ->
                    LevelAccuracyRow(level)
                    if (index < levels.lastIndex) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant,
                            thickness = Theme.dimensions.hairlineThickness,
                        )
                    }
                }
            }
        }

    state.heatmap.onLoaded { days ->
        if (days.isNotEmpty()) {
            Text(
                stringResource(Res.string.insights_activity_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                stringResource(
                    Res.string.insights_activity_summary,
                    days.size,
                    days.sumOf { it.count },
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LevelAccuracyRow(level: AccuracyByLevel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Theme.spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.xs),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(Res.string.insights_level_format, level.level),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(
                stringResource(
                    Res.string.insights_accuracy_reviews_format,
                    level.accuracyPercent.roundToInt(),
                    level.totalReviews.toInt(),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LinearProgressIndicator(
            progress = { (level.accuracyPercent / 100.0).toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .height(Theme.spacing.xs)
                .clip(RoundedCornerShape(Theme.shapes.pill)),
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            strokeCap = StrokeCap.Round,
        )
    }
}

// endregion

// region Words

@Composable
private fun WordsTab(state: InsightsState) {
    state.difficultWords
        .onLoading { LoadingScreen(message = stringResource(Res.string.insights_loading_words)) }
        .onError { msg, _ -> ErrorScreen(message = msg) }
        .onLoaded { words ->
            if (words.isEmpty()) {
                Text(
                    stringResource(Res.string.insights_no_difficult_words),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    stringResource(Res.string.insights_most_difficult_words),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(Theme.spacing.xs))
                words.forEachIndexed { index, word ->
                    DifficultWordRow(word)
                    if (index < words.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = Theme.spacing.xl),
                            color = MaterialTheme.colorScheme.outlineVariant,
                            thickness = Theme.dimensions.hairlineThickness,
                        )
                    }
                }
            }
        }
}

@Composable
private fun DifficultWordRow(word: WordDifficulty) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Theme.spacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                word.wordText,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            Text(
                word.wordTranslation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.width(Theme.spacing.sm))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                stringResource(Res.string.insights_error_rate_format, (word.errorRate * 100).roundToInt()),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.error,
            )
            Text(
                stringResource(Res.string.insights_reviews_format, word.totalReviews),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// endregion

// region Shared components

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconTint: Color,
    label: String,
    value: String,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Theme.dimensions.cardCornerRadius),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = Theme.elevation.low,
    ) {
        Column(modifier = Modifier.padding(Theme.spacing.lg)) {
            Box(
                modifier = Modifier
                    .size(Theme.dimensions.iconSizeHuge)
                    .clip(RoundedCornerShape(Theme.shapes.medium))
                    .background(iconTint.copy(alpha = Theme.opacity.focus)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(Theme.dimensions.iconSize),
                )
            }
            Spacer(modifier = Modifier.height(Theme.spacing.md))
            Text(
                value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(Theme.spacing.xxs))
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MetricRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    value: String,
    subtitle: String? = null,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Theme.dimensions.cardCornerRadius),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = Theme.elevation.low,
    ) {
        Row(
            modifier = Modifier.padding(Theme.spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Theme.spacing.md),
        ) {
            Box(
                modifier = Modifier
                    .size(Theme.dimensions.iconSizeHuge)
                    .clip(RoundedCornerShape(Theme.shapes.medium))
                    .background(iconTint.copy(alpha = Theme.opacity.focus)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(Theme.dimensions.iconSize),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    value,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                if (subtitle != null) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

// endregion

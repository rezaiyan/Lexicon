package feature.insights.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import components.ErrorScreen
import components.LoadingScreen
import components.Pill
import components.scaffold.LexiconColumn
import components.scaffold.TopBarColor
import core.common.UiState
import core.common.onError
import core.common.onLoaded
import core.common.onLoading
import domain.analytics.model.AccuracyByLevel
import domain.analytics.model.StudyHeatmapDay
import domain.analytics.model.StudyInsights
import domain.analytics.model.WordDifficulty
import feature.insights.InsightsState
import feature.insights.InsightsViewModel
import kotlin.math.roundToInt
import kotlin.time.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.insights_accuracy
import lexicon.resources.generated.resources.insights_accuracy_by_level
import lexicon.resources.generated.resources.insights_empty_subtitle
import lexicon.resources.generated.resources.insights_empty_title
import lexicon.resources.generated.resources.insights_accuracy_format
import lexicon.resources.generated.resources.insights_accuracy_reviews_format
import lexicon.resources.generated.resources.insights_best_study_time
import lexicon.resources.generated.resources.insights_cards_reviewed
import lexicon.resources.generated.resources.insights_days_studied
import lexicon.resources.generated.resources.insights_error_rate_format
import lexicon.resources.generated.resources.insights_level_format
import lexicon.resources.generated.resources.insights_loading
import lexicon.resources.generated.resources.insights_loading_levels
import lexicon.resources.generated.resources.insights_loading_words
import lexicon.resources.generated.resources.insights_most_difficult_words
import lexicon.resources.generated.resources.insights_reviews_format
import lexicon.resources.generated.resources.insights_sessions_words
import lexicon.resources.generated.resources.insights_this_week
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
        onNavigateBack = onNavigateBack,
        onDismissInsight = { viewModel.dismissDailyInsight() },
    )
}

@Composable
internal fun InsightsContent(
    state: InsightsState,
    onNavigateBack: () -> Unit,
    onDismissInsight: () -> Unit = {},
) {
    LexiconColumn(
        title = stringResource(Res.string.insights_title),
        showNavigationIcon = true,
        navigationIcon = Icons.Default.Close,
        onNavigationClick = onNavigateBack,
        scrollable = false,
        topBarColor = TopBarColor.Background,
    ) {
        if (!state.isLoaded) {
            LoadingScreen(message = stringResource(Res.string.insights_loading))
        } else if (!state.availability.hasAnyContent) {
            EmptyInsightsContent()
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Theme.spacing.md),
                verticalArrangement = Arrangement.spacedBy(Theme.spacing.lg),
            ) {
                state.dailyInsight?.let { message ->
                    DailyInsightBanner(message = message, onDismiss = onDismissInsight)
                }
                OverviewTab(state)
                TrendsTab(state)
                WordsTab(state)
                Spacer(modifier = Modifier.height(Theme.spacing.xl))
            }
        }
    }
}

// region Empty State

@Composable
private fun EmptyInsightsContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = Theme.spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Theme.spacing.sm),
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(40.dp),
                )
            }
            Spacer(modifier = Modifier.height(Theme.spacing.xs))
            Text(
                text = stringResource(Res.string.insights_empty_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(Res.string.insights_empty_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// endregion

// region Daily Insight Banner

@Composable
private fun DailyInsightBanner(message: String, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Theme.shapes.medium))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = Theme.spacing.md, vertical = Theme.spacing.sm),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.sm),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Daily Insight",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Spacer(modifier = Modifier.height(Theme.spacing.xxxs))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
        IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(Theme.dimensions.iconSize),
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Dismiss",
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

// endregion

// region Overview

@Composable
private fun OverviewTab(state: InsightsState) {
    state.overview
        .onLoading { LoadingScreen(message = stringResource(Res.string.insights_loading)) }
        .onError { msg, _ -> ErrorScreen(message = msg) }
        .onLoaded { insights ->
            if (state.availability.hasOverview) {
                HeroSection(insights)
                Spacer(modifier = Modifier.height(Theme.spacing.sm))
                OverviewCards(insights)
            }
        }

    state.bestStudyTime.onLoaded { bestTime ->
        if (bestTime != null && state.availability.hasOverview) {
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
private fun HeroSection(insights: StudyInsights) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Theme.spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SectionLabel(stringResource(Res.string.insights_cards_reviewed))
        Spacer(modifier = Modifier.height(Theme.spacing.xxs))
        Text(
            text = insights.totalCardsReviewed.toString(),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
        )
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
            icon = Icons.AutoMirrored.Filled.TrendingUp,
            iconTint = MaterialTheme.colorScheme.secondary,
            label = stringResource(Res.string.insights_accuracy),
            value = "${insights.accuracyPercent.roundToInt()}%",
        )
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
            if (levels.isNotEmpty()) {
                SectionLabel(stringResource(Res.string.insights_accuracy_by_level))
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

    val heatmapDays = when (val h = state.heatmap) {
        is UiState.Loaded -> h.value
        else -> emptyList()
    }
    ThisWeekSection(heatmapDays = heatmapDays)
}

@Composable
private fun LevelAccuracyRow(level: AccuracyByLevel) {
    val accuracyFraction = (level.accuracyPercent / 100.0).toFloat().coerceIn(0f, 1f)
    val primaryColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest

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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xs),
            ) {
                Text(
                    stringResource(
                        Res.string.insights_accuracy_reviews_format,
                        level.accuracyPercent.roundToInt(),
                        level.totalReviews.toInt(),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "${level.accuracyPercent.roundToInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        // Custom thin track using Box instead of LinearProgressIndicator
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(Theme.shapes.pill))
                .background(trackColor),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(accuracyFraction)
                    .height(4.dp)
                    .clip(RoundedCornerShape(Theme.shapes.pill))
                    .background(primaryColor),
            )
        }
    }
}

@Composable
private fun ThisWeekSection(heatmapDays: List<StudyHeatmapDay>) {
    val today = remember {
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    }
    val todayStr = remember(today) { today.toString() }
    val last7Days = remember(today, heatmapDays) {
        val countByDate = heatmapDays.associate { it.date to it.count }
        (6 downTo 0).map { daysAgo ->
            val date = today.minus(daysAgo, DateTimeUnit.DAY)
            StudyHeatmapDay(date = date.toString(), count = countByDate[date.toString()] ?: 0)
        }
    }
    val maxCount = last7Days.maxOfOrNull { it.count }.takeIf { it != null && it > 0 } ?: 1

    SectionLabel(stringResource(Res.string.insights_this_week))
    Spacer(Modifier.height(Theme.spacing.xs))
    // Bars float directly on the background — no Surface container
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Theme.spacing.md),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom,
    ) {
        last7Days.forEachIndexed { index, day ->
            WeekDayBar(day = day, maxCount = maxCount, todayStr = todayStr, index = index)
        }
    }
}

@Composable
private fun WeekDayBar(day: StudyHeatmapDay, maxCount: Int, todayStr: String, index: Int) {
    val isToday = day.date == todayStr
    val fraction = day.count.toFloat() / maxCount
    val targetFraction = fraction.coerceAtLeast(0.15f)
    val animatedFraction = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay((index * 60).toLong())
        animatedFraction.animateTo(
            targetValue = targetFraction,
            animationSpec = tween(500, easing = FastOutSlowInEasing),
        )
    }

    val barHeight = 120.dp * animatedFraction.value
    val shadowHeight = 120.dp * (animatedFraction.value * 1.5f).coerceAtMost(1f)
    val primaryColor = MaterialTheme.colorScheme.primary
    val shadowColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val barAlpha = if (isToday) 1f else (0.4f + fraction * 0.6f)

    val dayLabel = remember(day.date) {
        try {
            LocalDate.parse(day.date).dayOfWeek.name.take(3)
        } catch (_: Exception) {
            "???"
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (day.count > 0) {
            Text(
                day.count.toString(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Theme.spacing.xxxs))
        }
        Box(contentAlignment = Alignment.BottomCenter) {
            Box(
                Modifier
                    .width(32.dp)
                    .height(shadowHeight)
                    .clip(RoundedCornerShape(50))
                    .background(shadowColor),
            )
            Box(
                Modifier
                    .width(32.dp)
                    .height(barHeight)
                    .clip(RoundedCornerShape(50))
                    .background(primaryColor.copy(alpha = barAlpha)),
            )
        }
        Spacer(Modifier.height(Theme.spacing.xs))
        Text(
            dayLabel,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
            color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
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
            if (words.isNotEmpty()) {
                SectionLabel(stringResource(Res.string.insights_most_difficult_words))
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
    val errorPercent = (word.errorRate * 100).roundToInt()
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
            // Error rate as styled pill using design-system Pill component
            Pill(
                text = stringResource(Res.string.insights_error_rate_format, errorPercent),
                color = MaterialTheme.colorScheme.error,
                backgroundColor = MaterialTheme.colorScheme.errorContainer,
                height = 22.dp,
                cornerRadius = Theme.shapes.extraSmall,
            )
            Spacer(modifier = Modifier.height(Theme.spacing.xxs))
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

/**
 * Section label: labelMedium ALL CAPS + letter spacing + onSurfaceVariant.
 * Used as a lightweight alternative to titleMedium for section headers.
 */
@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        letterSpacing = 1.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/**
 * Stat card: no Surface/elevation — thin border + big bold number above small label.
 */
@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconTint: Color,
    label: String,
    value: String,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = Theme.dimensions.borderWidth,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(Theme.shapes.medium),
            )
            .padding(Theme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.xxs),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(Theme.dimensions.iconSize),
        )
        Text(
            value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Metric row: 3dp accent left bar + label above + bold value below — no Surface container.
 */
@Composable
private fun MetricRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    value: String,
    subtitle: String? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.md),
    ) {
        // 3dp accent left bar
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(if (subtitle != null) 56.dp else 40.dp)
                .clip(RoundedCornerShape(Theme.shapes.pill))
                .background(iconTint),
        )
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(Theme.dimensions.iconSize),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.labelMedium,
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
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// endregion

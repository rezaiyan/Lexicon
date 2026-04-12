package feature.insights.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.EmojiEvents
import components.scaffold.ActionIconConfig
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import components.ErrorScreen
import components.GradientProgressBar
import components.LoadingScreen
import components.LottieMotionIcon
import components.Pill
import components.animation.rememberAnimatedCounter
import components.scaffold.LexiconColumn
import components.scaffold.TopBarColor
import core.common.UiState
import core.common.onError
import core.common.onLoaded
import core.common.onLoading
import domain.analytics.model.AccuracyByLevel
import domain.analytics.model.HourlyAccuracy
import domain.wordrush.model.WordRushInsights
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
import lexicon.resources.generated.resources.best_streak
import lexicon.resources.generated.resources.day_streak
import lexicon.resources.generated.resources.insights_accuracy
import lexicon.resources.generated.resources.insights_accuracy_by_level
import lexicon.resources.generated.resources.insights_empty_subtitle
import lexicon.resources.generated.resources.insights_empty_title
import lexicon.resources.generated.resources.insights_best_study_time
import lexicon.resources.generated.resources.insights_cards_reviewed
import lexicon.resources.generated.resources.insights_days_studied
import lexicon.resources.generated.resources.insights_level_format
import lexicon.resources.generated.resources.leaderboard
import lexicon.resources.generated.resources.insights_load_error
import lexicon.resources.generated.resources.insights_loading
import lexicon.resources.generated.resources.insights_loading_levels
import lexicon.resources.generated.resources.insights_loading_words
import lexicon.resources.generated.resources.insights_most_difficult_words
import lexicon.resources.generated.resources.insights_reviews_format
import lexicon.resources.generated.resources.insights_sessions_words
import lexicon.resources.generated.resources.insights_this_week
import lexicon.resources.generated.resources.insights_title
import lexicon.resources.generated.resources.weekly_report_accuracy
import lexicon.resources.generated.resources.weekly_report_best_day
import lexicon.resources.generated.resources.weekly_report_cards_reviewed
import lexicon.resources.generated.resources.weekly_report_sessions
import lexicon.resources.generated.resources.insights_total_study_time
import lexicon.resources.generated.resources.retry
import lexicon.resources.generated.resources.insights_words_mastered
import lexicon.resources.generated.resources.word_rush_insights_avg_accuracy
import lexicon.resources.generated.resources.word_rush_insights_avg_score
import lexicon.resources.generated.resources.word_rush_insights_best_streak
import lexicon.resources.generated.resources.word_rush_insights_completion_rate
import lexicon.resources.generated.resources.word_rush_insights_title
import lexicon.resources.generated.resources.word_rush_insights_total_time
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import theme.AppColors
import theme.Theme

@Composable
fun InsightsScreen(
    onNavigateBack: () -> Unit,
    onShowLeaderboard: () -> Unit = {},
) {
    val viewModel = koinViewModel<InsightsViewModel>()
    val state by viewModel.state()

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    InsightsContent(
        state = state,
        onNavigateBack = onNavigateBack,
        onShowLeaderboard = onShowLeaderboard,
        onDismissInsight = { viewModel.dismissDailyInsight() },
        onRetry = { viewModel.refresh() },
    )
}

@Composable
internal fun InsightsContent(
    state: InsightsState,
    onNavigateBack: () -> Unit,
    onShowLeaderboard: () -> Unit = {},
    onDismissInsight: () -> Unit = {},
    onRetry: () -> Unit = {},
) {
    LexiconColumn(
        title = stringResource(Res.string.insights_title),
        showNavigationIcon = true,
        navigationIcon = Icons.Default.Close,
        onNavigationClick = onNavigateBack,
        scrollable = false,
        topBarColor = TopBarColor.Background,
        actionIcon1 = ActionIconConfig(
            icon = Icons.Rounded.EmojiEvents,
            contentDescription = stringResource(Res.string.leaderboard),
            onClick = onShowLeaderboard,
            size = Theme.dimensions.iconSize,
        ),
    ) {
        if (!state.isLoaded) {
            LoadingScreen(message = stringResource(Res.string.insights_loading))
        } else if (state.isError) {
            ErrorScreen(
                message = stringResource(Res.string.insights_load_error),
                retryLabel = stringResource(Res.string.retry),
                onRetry = onRetry,
            )
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
                val currentStreak = state.currentStreak
                if (currentStreak != null) {
                    StreakCard(
                        currentStreak = currentStreak,
                        longestStreak = state.longestStreak,
                    )
                }
                val weeklyReport = (state.weeklyReport as? UiState.Loaded)?.value
                if (weeklyReport is feature.insights.WeeklyReportUiModel.Content) {
                    WeeklyReportCard(report = weeklyReport)
                }
                OverviewTab(state, onDismissInsight)
                if (state.availability.hasWordRush) {
                    WordRushInsightsSection(state)
                }
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

// region Streak

private const val FIRE_LOTTIE_URL =
    "https://assets-v2.lottiefiles.com/a/9d140e5e-1121-11ef-a147-0f8f2c5fd446/12M9FMZfjS.json"

@Composable
private fun StreakCard(
    currentStreak: Int,
    longestStreak: Int?,
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val animatedCurrent = rememberAnimatedCounter(target = currentStreak)
    val animatedLongest = rememberAnimatedCounter(target = longestStreak ?: 0)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(surfaceColor.copy(alpha = 0.9f))
            .padding(start = Theme.spacing.xxs, end = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        Box(
            modifier = Modifier
                .padding(bottom = Theme.spacing.sm)
                .size(64.dp),
            contentAlignment = Alignment.Center,
        ) {
            LottieMotionIcon(url = FIRE_LOTTIE_URL, modifier = Modifier.size(64.dp))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$animatedCurrent",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = primaryColor,
                lineHeight = 30.sp,
            )
            Spacer(modifier = Modifier.height(Theme.spacing.xxxs))
            Text(
                text = stringResource(Res.string.day_streak),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.3.sp,
            )
        }
        if (longestStreak != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$animatedLongest",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = tertiaryColor,
                    lineHeight = 30.sp,
                )
                Spacer(modifier = Modifier.height(Theme.spacing.xxxs))
                Text(
                    text = stringResource(Res.string.best_streak),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.3.sp,
                )
            }
        }
    }
}

// endregion

// region Weekly Report

@Composable
private fun WeeklyReportCard(
    report: feature.insights.WeeklyReportUiModel.Content,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Theme.shapes.large))
            .background(Theme.gradients.primaryWash)
            .padding(Theme.spacing.md),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.md)) {
            // Header: title + date range label + optional change badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = stringResource(Res.string.insights_this_week).uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = report.weekRangeLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (report.changeLabel != null) {
                    val badgeColor = if (report.isChangePositive) AppColors.accentEmerald else AppColors.error
                    Pill(
                        text = report.changeLabel,
                        color = badgeColor,
                        backgroundColor = badgeColor.copy(alpha = 0.12f),
                    )
                }
            }

            // Stats row: cards reviewed (hero) + accuracy + sessions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                WeeklyStatCell(
                    value = report.cardsReviewed,
                    label = stringResource(Res.string.weekly_report_cards_reviewed),
                    color = MaterialTheme.colorScheme.primary,
                    isHero = true,
                )
                WeeklyStatCell(
                    value = report.accuracyValue,
                    label = stringResource(Res.string.weekly_report_accuracy),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                WeeklyStatCell(
                    value = report.sessionsValue,
                    label = stringResource(Res.string.weekly_report_sessions),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            // Optional best day label
            if (report.bestDayLabel != null) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xs),
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = "${stringResource(Res.string.weekly_report_best_day)}: ${report.bestDayLabel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun WeeklyStatCell(
    value: String,
    label: String,
    color: Color,
    isHero: Boolean = false,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.xxxs),
    ) {
        Text(
            text = value,
            style = if (isHero) MaterialTheme.typography.displaySmall else MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

// endregion

// region Overview

@Composable
private fun OverviewTab(state: InsightsState, onDismissInsight: () -> Unit) {
    state.overview
        .onLoading { LoadingScreen(message = stringResource(Res.string.insights_loading)) }
        .onError { msg, _ -> ErrorScreen(message = msg) }
        .onLoaded { insights ->
            if (state.availability.hasOverview) {
                val bestTime = (state.bestStudyTime as? UiState.Loaded)?.value
                val heatmapDays = (state.heatmap as? UiState.Loaded)?.value ?: emptyList()
                StudyInsightsCard(
                    insights = insights,
                    bestStudyTime = bestTime,
                    heatmapDays = heatmapDays,
                    dailyInsight = state.dailyInsight,
                    onDismissInsight = onDismissInsight,
                )
            }
        }
}

@Composable
private fun StudyInsightsCard(
    insights: StudyInsights,
    bestStudyTime: HourlyAccuracy?,
    heatmapDays: List<StudyHeatmapDay>,
    dailyInsight: String?,
    onDismissInsight: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val chevronDegrees by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
    )
    val tint = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Theme.shapes.large))
            .background(Theme.gradients.primaryWash)
            .clickable { expanded = !expanded }
            .padding(Theme.spacing.md),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.md)) {
            // Header: icon + label / sessions pill + chevron
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xs),
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(tint.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                            contentDescription = null,
                            tint = tint,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    Text(
                        text = stringResource(Res.string.insights_cards_reviewed).uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xs),
                ) {
                    Pill(
                        text = "${insights.totalSessions.toInt()} sessions",
                        color = tint,
                        backgroundColor = tint.copy(alpha = 0.12f),
                    )
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(Theme.dimensions.iconSize)
                            .rotate(chevronDegrees),
                    )
                }
            }
            // Hero: total cards reviewed (always visible)
            Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.xxxs)) {
                Text(
                    text = insights.totalCardsReviewed.toString(),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = tint,
                )
                Text(
                    text = "${insights.accuracyPercent.roundToInt()}% accuracy",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            // Daily insight (always visible when present)
            if (dailyInsight != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(Theme.shapes.small))
                        .background(tint.copy(alpha = 0.10f))
                        .padding(horizontal = Theme.spacing.sm, vertical = Theme.spacing.xs),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xs),
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .size(14.dp),
                    )
                    Text(
                        text = dailyInsight,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { onDismissInsight() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp),
                        )
                    }
                }
            }
            // Expandable details
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(tween(durationMillis = 300, easing = FastOutSlowInEasing)),
                exit = shrinkVertically(tween(durationMillis = 300, easing = FastOutSlowInEasing)),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.md)) {
                    HorizontalDivider(color = tint.copy(alpha = 0.15f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        InsightsStatCell(
                            value = "${insights.accuracyPercent.roundToInt()}%",
                            label = stringResource(Res.string.insights_accuracy),
                            tint = MaterialTheme.colorScheme.secondary,
                        )
                        InsightsStatCell(
                            value = insights.wordsMasteredCount.toString(),
                            label = stringResource(Res.string.insights_words_mastered),
                            tint = MaterialTheme.colorScheme.tertiary,
                        )
                        InsightsStatCell(
                            value = insights.daysStudied.toString(),
                            label = stringResource(Res.string.insights_days_studied),
                            tint = tint,
                        )
                    }
                    HorizontalDivider(color = tint.copy(alpha = 0.15f))
                    val totalTimeMinutes = insights.totalStudyTimeMs / 60_000
                    val hours = totalTimeMinutes / 60
                    val minutes = totalTimeMinutes % 60
                    InsightsFooterRow(
                        icon = Icons.Default.Schedule,
                        title = stringResource(Res.string.insights_total_study_time),
                        value = if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m",
                        subtitle = stringResource(
                            Res.string.insights_sessions_words,
                            insights.totalSessions.toInt(),
                            insights.uniqueWordsReviewed.toInt(),
                        ),
                    )
                    if (bestStudyTime != null) {
                        InsightsFooterRow(
                            icon = Icons.Default.Schedule,
                            title = stringResource(Res.string.insights_best_study_time),
                            value = "${bestStudyTime.hour}:00",
                            subtitle = "${bestStudyTime.accuracyPercent.roundToInt()}% accuracy",
                        )
                    }
                    if (heatmapDays.isNotEmpty()) {
                        HorizontalDivider(color = tint.copy(alpha = 0.15f))
                        ThisWeekSection(heatmapDays = heatmapDays)
                    }
                }
            }
        }
    }
}

// endregion

// region Word Rush

@Composable
private fun WordRushInsightsSection(state: InsightsState) {
    state.wordRushInsights.onLoaded { insights ->
        if (insights.totalGames > 0) {
            WordRushInsightsCard(insights)
        }
    }
}

@Composable
private fun WordRushInsightsCard(insights: WordRushInsights) {
    var expanded by remember { mutableStateOf(false) }
    val chevronDegrees by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Theme.shapes.large))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        AppColors.tertiary.copy(alpha = 0.14f),
                        AppColors.accentAmber.copy(alpha = 0.08f),
                    ),
                ),
            )
            .clickable { expanded = !expanded }
            .padding(Theme.spacing.md),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.md)) {
            // Header: icon + title / games pill + chevron
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xs),
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(AppColors.tertiary.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Bolt,
                            contentDescription = null,
                            tint = AppColors.tertiary,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    Text(
                        text = stringResource(Res.string.word_rush_insights_title).uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xs),
                ) {
                    Pill(
                        text = "${insights.totalGames} games",
                        color = AppColors.tertiary,
                        backgroundColor = AppColors.tertiary.copy(alpha = 0.12f),
                    )
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(Theme.dimensions.iconSize)
                            .rotate(chevronDegrees),
                    )
                }
            }
            // Hero: best streak (always visible)
            Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.xxxs)) {
                Text(
                    text = insights.bestStreakEver.toString(),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.tertiary,
                )
                Text(
                    text = stringResource(Res.string.word_rush_insights_best_streak),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            // Expandable details
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(tween(durationMillis = 300, easing = FastOutSlowInEasing)),
                exit = shrinkVertically(tween(durationMillis = 300, easing = FastOutSlowInEasing)),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.md)) {
                    HorizontalDivider(color = AppColors.tertiary.copy(alpha = 0.15f))
                    // 3-col stat row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        InsightsStatCell(
                            value = "${insights.avgAccuracyPercent.toOneDecimalString()}%",
                            label = stringResource(Res.string.word_rush_insights_avg_accuracy),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        InsightsStatCell(
                            value = insights.avgScore.toOneDecimalString(),
                            label = stringResource(Res.string.word_rush_insights_avg_score),
                            tint = AppColors.tertiary,
                        )
                        InsightsStatCell(
                            value = "${insights.completionRatePercent.toOneDecimalString()}%",
                            label = stringResource(Res.string.word_rush_insights_completion_rate),
                            tint = MaterialTheme.colorScheme.secondary,
                        )
                    }
                    HorizontalDivider(color = AppColors.tertiary.copy(alpha = 0.15f))
                    InsightsFooterRow(
                        icon = Icons.Default.Schedule,
                        title = stringResource(Res.string.word_rush_insights_total_time),
                        value = "${insights.totalTimePlayedMs / 60000} min",
                    )
                }
            }
        }
    }
}

@Composable
private fun InsightsStatCell(
    value: String,
    label: String,
    tint: Color,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.xxxs),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
        )
    }
}

// endregion

// region Helpers

private fun Double.toOneDecimalString(): String {
    val rounded = kotlin.math.round(this * 10) / 10.0
    val whole = rounded.toLong()
    val decimal = kotlin.math.round((rounded - whole) * 10).toInt()
    return "$whole.$decimal"
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
                AccuracyByLevelCard(levels)
            }
        }

}

@Composable
private fun AccuracyByLevelCard(levels: List<AccuracyByLevel>) {
    var expanded by remember { mutableStateOf(false) }
    val chevronDegrees by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
    )
    val tint = MaterialTheme.colorScheme.secondary
    val avgAccuracy = remember(levels) { levels.map { it.accuracyPercent }.average() }
    val totalReviews = remember(levels) { levels.sumOf { it.totalReviews } }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Theme.shapes.large))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        AppColors.secondary.copy(alpha = 0.14f),
                        AppColors.accentEmerald.copy(alpha = 0.08f),
                    ),
                ),
            )
            .clickable { expanded = !expanded }
            .padding(Theme.spacing.md),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.md)) {
            // Header: icon + label / levels pill + chevron
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xs),
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(tint.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = null,
                            tint = tint,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    Text(
                        text = stringResource(Res.string.insights_accuracy_by_level).uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xs),
                ) {
                    Pill(
                        text = "${levels.size} levels",
                        color = tint,
                        backgroundColor = tint.copy(alpha = 0.12f),
                    )
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(Theme.dimensions.iconSize)
                            .rotate(chevronDegrees),
                    )
                }
            }
            // Hero: average accuracy (always visible)
            Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.xxxs)) {
                Text(
                    text = "${avgAccuracy.roundToInt()}%",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = tint,
                )
                Text(
                    text = stringResource(Res.string.insights_reviews_format, totalReviews.toInt()),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            // Expandable level rows
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(tween(durationMillis = 300, easing = FastOutSlowInEasing)),
                exit = shrinkVertically(tween(durationMillis = 300, easing = FastOutSlowInEasing)),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.xxxs)) {
                    HorizontalDivider(color = tint.copy(alpha = 0.15f))
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
        }
    }
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
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xs),
            ) {
                Text(
                    stringResource(Res.string.insights_reviews_format, level.totalReviews.toInt()),
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
        GradientProgressBar(
            progress = accuracyFraction,
            gradientColors = listOf(primaryColor.copy(alpha = 0.7f), primaryColor),
            trackColor = trackColor,
            height = 6.dp,
            modifier = Modifier.clip(RoundedCornerShape(Theme.shapes.pill)),
        )
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

    Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.sm)) {
        SectionLabel(stringResource(Res.string.insights_this_week))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Theme.spacing.md),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom,
            ) {
                last7Days.forEachIndexed { index, day ->
                    WeekDayBar(day = day, maxCount = maxCount, todayStr = todayStr, index = index)
                }
            }
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
                Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.sm)) {
                    SectionLabel(stringResource(Res.string.insights_most_difficult_words))
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
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                word.wordTranslation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(modifier = Modifier.width(Theme.spacing.sm))
        Column(horizontalAlignment = Alignment.End) {
            // Error rate as styled pill using design-system Pill component
            Pill(
                text = "$errorPercent% error",
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

@Composable
private fun InsightsFooterRow(
    icon: ImageVector,
    title: String,
    value: String,
    subtitle: String? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xs),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(Theme.dimensions.iconSize),
            )
            Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.xxxs)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                }
            }
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/**
 * Section label: dot accent + labelMedium ALL CAPS + letter spacing + onSurfaceVariant.
 */
@Composable
private fun SectionLabel(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xs),
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
        )
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// endregion

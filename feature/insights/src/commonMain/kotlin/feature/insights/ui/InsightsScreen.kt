package feature.insights.ui

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.EmojiEvents
import components.scaffold.ActionIconConfig
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
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
import components.LoadingScreen
import components.Pill
import components.scaffold.LexiconColumn
import components.scaffold.TopBarColor
import core.common.UiState
import core.common.onError
import core.common.onLoaded
import core.common.onLoading
import domain.analytics.model.HourlyAccuracy
import domain.wordrush.model.WordRushInsights
import domain.analytics.model.StudyInsights
import domain.analytics.model.WordDifficulty
import events.OnEvents
import feature.insights.InsightsEffect
import feature.insights.InsightsState
import feature.insights.InsightsViewModel
import kotlin.math.roundToInt
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.insights_accuracy
import lexicon.resources.generated.resources.insights_empty_subtitle
import lexicon.resources.generated.resources.insights_empty_title
import lexicon.resources.generated.resources.insights_best_study_time
import lexicon.resources.generated.resources.insights_cards_reviewed
import lexicon.resources.generated.resources.insights_days_studied
import lexicon.resources.generated.resources.leaderboard
import lexicon.resources.generated.resources.insights_load_error
import lexicon.resources.generated.resources.insights_loading
import lexicon.resources.generated.resources.insights_loading_words
import lexicon.resources.generated.resources.insights_set_reminder
import lexicon.resources.generated.resources.insights_study_these_words
import lexicon.resources.generated.resources.insights_most_difficult_words
import lexicon.resources.generated.resources.insights_reviews_format
import lexicon.resources.generated.resources.insights_sessions_words
import lexicon.resources.generated.resources.insights_title
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
    onShowLeaderboard: () -> Unit = {},
    onNavigateToReview: (List<Long>) -> Unit = {},
    onNavigateToNotificationSettings: () -> Unit = {},
) {
    val viewModel = koinViewModel<InsightsViewModel>()
    val state by viewModel.state()

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    OnEvents(viewModel.effects) { effect ->
        when (effect) {
            is InsightsEffect.NavigateToReviewWithWords -> onNavigateToReview(effect.wordIds)
            InsightsEffect.NavigateToNotificationSettings -> onNavigateToNotificationSettings()
        }
    }

    InsightsContent(
        state = state,
        onShowLeaderboard = onShowLeaderboard,
        onDismissInsight = { viewModel.dismissDailyInsight() },
        onRetry = { viewModel.refresh() },
        onStudyDifficultWords = viewModel::studyDifficultWords,
        onSetReminder = viewModel::setReminder,
    )
}

@Composable
internal fun InsightsContent(
    state: InsightsState,
    onShowLeaderboard: () -> Unit = {},
    onDismissInsight: () -> Unit = {},
    onRetry: () -> Unit = {},
    onStudyDifficultWords: () -> Unit = {},
    onSetReminder: (Boolean) -> Unit = {},
) {
    LexiconColumn(
        title = stringResource(Res.string.insights_title),
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
                    .padding(top = Theme.spacing.xs),
                verticalArrangement = Arrangement.spacedBy(Theme.spacing.md),
            ) {
                val heatmapDays = (state.heatmap as? UiState.Loaded)?.value ?: emptyList()
                val week = rememberLastSevenDays(heatmapDays)
                val weeklyReport = (state.weeklyReport as? UiState.Loaded)?.value
                    as? feature.insights.WeeklyReportUiModel.Content

                val currentStreak = state.currentStreak
                if (currentStreak != null) {
                    StreakWeekCard(
                        currentStreak = currentStreak,
                        longestStreak = state.longestStreak,
                        week = week,
                    )
                }
                if (weeklyReport != null) {
                    WeeklyStatTiles(
                        cardsReviewed = weeklyReport.cardsReviewed,
                        accuracy = weeklyReport.accuracyValue,
                        changeLabel = weeklyReport.changeLabel,
                        isChangePositive = weeklyReport.isChangePositive,
                    )
                }
                if (heatmapDays.isNotEmpty()) {
                    ReviewsPerDayCard(
                        week = week,
                        sessions = weeklyReport?.sessionsValue,
                        bestDay = weeklyReport?.bestDayLabel,
                    )
                }
                OverviewTab(state, onDismissInsight, onSetReminder)
                if (state.availability.hasWordRush) {
                    WordRushInsightsSection(state)
                }
                TrendsTab(state)
                WordsTab(state, onStudyDifficultWords)
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

// region Overview

@Composable
private fun OverviewTab(state: InsightsState, onDismissInsight: () -> Unit, onSetReminder: (Boolean) -> Unit = {}) {
    state.overview
        .onLoading { LoadingScreen(message = stringResource(Res.string.insights_loading)) }
        .onError { msg, _ -> ErrorScreen(message = msg) }
        .onLoaded { insights ->
            if (state.availability.hasOverview) {
                val bestTime = (state.bestStudyTime as? UiState.Loaded)?.value
                StudyInsightsCard(
                    insights = insights,
                    bestStudyTime = bestTime,
                    dailyInsight = state.dailyInsight,
                    reviewRemindersEnabled = state.reviewRemindersEnabled,
                    onDismissInsight = onDismissInsight,
                    onSetReminder = onSetReminder,
                )
            }
        }
}

@Composable
private fun StudyInsightsCard(
    insights: StudyInsights,
    bestStudyTime: HourlyAccuracy?,
    dailyInsight: String?,
    reviewRemindersEnabled: Boolean = false,
    onDismissInsight: () -> Unit,
    onSetReminder: (Boolean) -> Unit = {},
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = stringResource(Res.string.insights_set_reminder),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Switch(
                                checked = reviewRemindersEnabled,
                                onCheckedChange = onSetReminder,
                            )
                        }
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
internal fun InsightsStatCell(
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
private fun WordsTab(state: InsightsState, onStudyDifficultWords: () -> Unit = {}) {
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
                    Button(
                        onClick = onStudyDifficultWords,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(Res.string.insights_study_these_words))
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
internal fun InsightsFooterRow(
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

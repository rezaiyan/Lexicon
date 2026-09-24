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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Schedule
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import components.ErrorScreen
import components.GradientProgressBar
import components.LoadingScreen
import components.Pill
import core.common.onError
import core.common.onLoaded
import core.common.onLoading
import domain.analytics.model.AccuracyByLevel
import domain.analytics.model.DayOfWeekAccuracy
import domain.analytics.model.LevelTransition
import domain.analytics.model.ResponseTimeTrend
import domain.analytics.model.StudyHeatmapDay
import feature.insights.InsightsState
import kotlin.math.roundToInt
import kotlin.time.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.insights_accuracy_by_level
import lexicon.resources.generated.resources.insights_level_format
import lexicon.resources.generated.resources.insights_level_from_to
import lexicon.resources.generated.resources.insights_level_transitions
import lexicon.resources.generated.resources.insights_level_transitions_demotions
import lexicon.resources.generated.resources.insights_level_transitions_promotions
import lexicon.resources.generated.resources.insights_level_transitions_words
import lexicon.resources.generated.resources.insights_loading_levels
import lexicon.resources.generated.resources.insights_response_time_avg_ms
import lexicon.resources.generated.resources.insights_response_time_declining
import lexicon.resources.generated.resources.insights_response_time_improving
import lexicon.resources.generated.resources.insights_response_time_stable
import lexicon.resources.generated.resources.insights_response_time_trend
import lexicon.resources.generated.resources.insights_response_time_week
import lexicon.resources.generated.resources.insights_reviews_format
import lexicon.resources.generated.resources.insights_this_week
import org.jetbrains.compose.resources.stringResource
import theme.AppColors
import theme.Theme
import utils.LexiconFormatters

// region Trends

@Composable
internal fun TrendsTab(state: InsightsState) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.md),
    ) {
        state.accuracyByLevel
            .onLoading { LoadingScreen(message = stringResource(Res.string.insights_loading_levels)) }
            .onError { msg, _ -> ErrorScreen(message = msg) }
            .onLoaded { levels ->
                if (levels.isNotEmpty()) {
                    AccuracyByLevelCard(levels)
                }
            }
        if (state.accuracyByDayOfWeek.isNotEmpty()) {
            DayOfWeekAccuracyChart(state.accuracyByDayOfWeek)
        }
        state.levelTransitions
            .onLoaded { transitions ->
                if (transitions.isNotEmpty()) {
                    LevelTransitionsCard(transitions)
                }
            }
        state.responseTimeTrend
            .onLoaded { trend ->
                if (trend.isNotEmpty()) {
                    ResponseTimeTrendCard(trend)
                }
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
private fun DayOfWeekAccuracyChart(days: List<DayOfWeekAccuracy>) {
    val currentDayOfWeek = remember {
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).dayOfWeek.ordinal + 1
    }
    val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val maxAccuracy = days.maxOfOrNull { it.accuracyPercent }.takeIf { (it ?: 0.0) > 0.0 } ?: 100.0

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Theme.shapes.large))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        AppColors.accentAmber.copy(alpha = 0.12f),
                        AppColors.secondary.copy(alpha = 0.06f),
                    ),
                ),
            )
            .padding(Theme.spacing.md),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.sm)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Filled.BarChart,
                        contentDescription = null,
                        tint = AppColors.accentAmber,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        "Accuracy by Day",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                days.forEach { day ->
                    val isToday = day.dayOfWeek == currentDayOfWeek
                    val fraction = if (maxAccuracy > 0.0) {
                        (day.accuracyPercent / maxAccuracy).toFloat().coerceIn(0f, 1f)
                    } else {
                        0f
                    }
                    val barColor = when {
                        day.totalReviews == 0L -> MaterialTheme.colorScheme.surfaceContainerHighest
                        day.accuracyPercent >= 80.0 -> AppColors.accentEmerald
                        day.accuracyPercent >= 60.0 -> AppColors.accentAmber
                        else -> AppColors.error
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                    ) {
                        if (day.totalReviews > 0L) {
                            Text(
                                "${day.accuracyPercent.roundToInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                color = barColor,
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                            )
                            Spacer(Modifier.height(2.dp))
                        }
                        val barPx = (fraction * 80f).dp
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 3.dp)
                                .height(if (barPx > 4.dp) barPx else 4.dp)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(if (isToday) barColor else barColor.copy(alpha = 0.65f)),
                        )
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                days.forEach { day ->
                    val isToday = day.dayOfWeek == currentDayOfWeek
                    Text(
                        dayNames[day.dayOfWeek - 1],
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                        color = if (isToday) AppColors.accentAmber else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }
        }
    }
}

@Composable
private fun LevelTransitionsCard(transitions: List<LevelTransition>) {
    var expanded by remember { mutableStateOf(false) }
    val chevronDegrees by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
    )
    val tint = AppColors.accentEmerald
    val promotions = remember(transitions) { transitions.filter { it.toLevel > it.fromLevel } }
    val demotions = remember(transitions) { transitions.filter { it.toLevel < it.fromLevel } }
    val promotionCount = remember(promotions) { promotions.sumOf { it.count } }
    val demotionCount = remember(demotions) { demotions.sumOf { it.count } }
    val sortedTransitions = remember(transitions) { transitions.sortedByDescending { it.count } }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Theme.shapes.large))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        tint.copy(alpha = 0.12f),
                        AppColors.secondary.copy(alpha = 0.06f),
                    ),
                ),
            )
            .clickable { expanded = !expanded }
            .padding(Theme.spacing.md),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.md)) {
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
                        text = stringResource(Res.string.insights_level_transitions).uppercase(),
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
                        text = "${transitions.size}",
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xl),
            ) {
                InsightsStatCell(
                    value = promotionCount.toString(),
                    label = stringResource(Res.string.insights_level_transitions_promotions),
                    tint = AppColors.accentEmerald,
                )
                InsightsStatCell(
                    value = demotionCount.toString(),
                    label = stringResource(Res.string.insights_level_transitions_demotions),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(tween(durationMillis = 300, easing = FastOutSlowInEasing)),
                exit = shrinkVertically(tween(durationMillis = 300, easing = FastOutSlowInEasing)),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.xxxs)) {
                    HorizontalDivider(color = tint.copy(alpha = 0.15f))
                    sortedTransitions.forEachIndexed { index, transition ->
                        LevelTransitionRow(transition)
                        if (index < sortedTransitions.lastIndex) {
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
private fun LevelTransitionRow(transition: LevelTransition) {
    val isPromotion = transition.toLevel > transition.fromLevel
    val rowColor = if (isPromotion) AppColors.accentEmerald else MaterialTheme.colorScheme.error
    val icon = if (isPromotion) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Theme.spacing.xs),
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
                tint = rowColor,
                modifier = Modifier.size(Theme.dimensions.iconSize),
            )
            Text(
                text = stringResource(Res.string.insights_level_from_to, transition.fromLevel, transition.toLevel),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Text(
            text = stringResource(Res.string.insights_level_transitions_words, transition.count.toInt()),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = rowColor,
        )
    }
}

@Composable
private fun ResponseTimeTrendCard(trend: List<ResponseTimeTrend>) {
    var expanded by remember { mutableStateOf(false) }
    val chevronDegrees by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
    )
    val tint = MaterialTheme.colorScheme.secondary
    val sortedTrend = remember(trend) { trend.sortedWith(compareBy({ it.year }, { it.week })) }
    val latest = sortedTrend.lastOrNull()
    val previous = sortedTrend.dropLast(1).lastOrNull()
    val trendLabel = remember(latest, previous) {
        when {
            latest == null || previous == null -> "stable"
            latest.avgResponseTimeMs < previous.avgResponseTimeMs * 0.95 -> "improving"
            latest.avgResponseTimeMs > previous.avgResponseTimeMs * 1.05 -> "declining"
            else -> "stable"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Theme.shapes.large))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        tint.copy(alpha = 0.14f),
                        AppColors.accentEmerald.copy(alpha = 0.06f),
                    ),
                ),
            )
            .clickable { expanded = !expanded }
            .padding(Theme.spacing.md),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.md)) {
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
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = tint,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    Text(
                        text = stringResource(Res.string.insights_response_time_trend).uppercase(),
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
                    val trendIcon = when (trendLabel) {
                        "improving" -> Icons.AutoMirrored.Filled.TrendingUp
                        "declining" -> Icons.AutoMirrored.Filled.TrendingDown
                        else -> null
                    }
                    if (trendIcon != null) {
                        Icon(
                            imageVector = trendIcon,
                            contentDescription = null,
                            tint = if (trendLabel == "improving") {
                                AppColors.accentEmerald
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                            modifier = Modifier.size(Theme.dimensions.iconSize),
                        )
                    }
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
            if (latest != null) {
                Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.xxxs)) {
                    Text(
                        text = stringResource(
                            Res.string.insights_response_time_avg_ms,
                            LexiconFormatters.secondsOneDecimal(latest.avgResponseTimeMs),
                        ),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = tint,
                    )
                    Text(
                        text = when (trendLabel) {
                            "improving" -> stringResource(Res.string.insights_response_time_improving)
                            "declining" -> stringResource(Res.string.insights_response_time_declining)
                            else -> stringResource(Res.string.insights_response_time_stable)
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(tween(durationMillis = 300, easing = FastOutSlowInEasing)),
                exit = shrinkVertically(tween(durationMillis = 300, easing = FastOutSlowInEasing)),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.xxxs)) {
                    HorizontalDivider(color = tint.copy(alpha = 0.15f))
                    sortedTrend.forEachIndexed { index, entry ->
                        InsightsFooterRow(
                            icon = Icons.Default.Schedule,
                            title = stringResource(Res.string.insights_response_time_week, entry.week),
                            value = stringResource(
                                Res.string.insights_response_time_avg_ms,
                                LexiconFormatters.secondsOneDecimal(entry.avgResponseTimeMs),
                            ),
                        )
                        if (index < sortedTrend.lastIndex) {
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
internal fun ThisWeekSection(heatmapDays: List<StudyHeatmapDay>) {
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

package feature.insights.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import components.Pill
import components.animation.rememberAnimatedCounter
import domain.analytics.model.StudyHeatmapDay
import kotlin.time.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.best_streak
import lexicon.resources.generated.resources.day_streak
import lexicon.resources.generated.resources.insights_this_week
import lexicon.resources.generated.resources.weekly_report_accuracy
import lexicon.resources.generated.resources.weekly_report_best_day
import lexicon.resources.generated.resources.weekly_report_cards_reviewed
import lexicon.resources.generated.resources.weekly_report_sessions
import org.jetbrains.compose.resources.stringResource
import theme.AppColors
import theme.Theme

@Composable
internal fun rememberLastSevenDays(heatmapDays: List<StudyHeatmapDay>): List<WeekDay> {
    val today = remember {
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    }
    return remember(today, heatmapDays) {
        val countByDate = heatmapDays.associate { it.date to it.count }
        (6 downTo 0).map { daysAgo ->
            val date: LocalDate = today.minus(daysAgo, DateTimeUnit.DAY)
            WeekDay(
                label = date.dayOfWeek.name.take(1),
                count = countByDate[date.toString()] ?: 0,
                isToday = daysAgo == 0,
            )
        }
    }
}

@Composable
private fun InsightsCard(
    modifier: Modifier = Modifier,
    padding: Dp = Theme.spacing.cardPadding,
    cornerRadius: Dp = Theme.shapes.large,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(cornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = Theme.elevation.low),
    ) {
        Box(modifier = Modifier.padding(padding)) { content() }
    }
}

@Composable
internal fun StreakWeekCard(
    currentStreak: Int,
    longestStreak: Int?,
    week: List<WeekDay>,
) {
    val animatedCurrent = rememberAnimatedCounter(target = currentStreak)
    InsightsCard(padding = Theme.spacing.heroPadding, cornerRadius = Theme.shapes.extraLarge - Theme.spacing.xxs) {
        Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xs),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Text(
                        text = "$animatedCurrent",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = stringResource(Res.string.day_streak),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = Theme.spacing.xxs),
                    )
                }
                if (longestStreak != null) {
                    Text(
                        text = "${stringResource(Res.string.best_streak)}: $longestStreak",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = Theme.spacing.xxs),
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                week.forEach { day -> StreakDayDot(day) }
            }
        }
    }
}

@Composable
private fun StreakDayDot(day: WeekDay) {
    val studied = day.count > 0
    val dotSize = Theme.spacing.xl
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.xs),
    ) {
        Box(
            modifier = Modifier
                .size(dotSize)
                .clip(CircleShape)
                .then(
                    if (studied) {
                        Modifier.background(MaterialTheme.colorScheme.primary)
                    } else {
                        Modifier.border(
                            BorderStroke(2.dp, MaterialTheme.colorScheme.outlineVariant),
                            CircleShape,
                        )
                    }
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (studied) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(Theme.dimensions.iconSizeSmall),
                )
            }
        }
        Text(
            text = day.label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal,
            color = if (day.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun WeeklyStatTiles(
    cardsReviewed: String,
    accuracy: String,
    changeLabel: String?,
    isChangePositive: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.md),
    ) {
        StatTile(
            value = cardsReviewed,
            label = stringResource(Res.string.weekly_report_cards_reviewed),
            modifier = Modifier.weight(1f),
            badge = changeLabel?.let { label ->
                {
                    val badgeColor = if (isChangePositive) AppColors.accentEmerald else AppColors.error
                    Pill(
                        text = label,
                        color = badgeColor,
                        backgroundColor = badgeColor.copy(alpha = 0.12f),
                    )
                }
            },
        )
        StatTile(
            value = accuracy,
            label = stringResource(Res.string.weekly_report_accuracy),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    badge: (@Composable () -> Unit)? = null,
) {
    InsightsCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.textGap)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                badge?.invoke()
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun ReviewsPerDayCard(
    week: List<WeekDay>,
    sessions: String?,
    bestDay: String?,
) {
    val maxCount = week.maxOfOrNull { it.count }?.takeIf { it > 0 } ?: 1
    val chartHeight = 120.dp
    val chartDescription = week.joinToString(prefix = "${stringResource(Res.string.insights_this_week)}: ") {
        "${it.label} ${it.count}"
    }

    InsightsCard {
        Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.md)) {
            Text(
                text = stringResource(Res.string.insights_this_week),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clearAndSetSemantics { contentDescription = chartDescription },
                horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xs),
                verticalAlignment = Alignment.Bottom,
            ) {
                week.forEach { day ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Theme.spacing.xxs),
                    ) {
                        Text(
                            text = if (day.count > 0) day.count.toString() else "",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        val barHeight = if (day.count > 0) {
                            chartHeight * (day.count.toFloat() / maxCount)
                        } else {
                            Theme.spacing.xxs
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(barHeight)
                                .clip(RoundedCornerShape(Theme.shapes.small - Theme.spacing.xxxs))
                                .background(
                                    if (day.count > 0) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outlineVariant
                                    }
                                ),
                        )
                        Text(
                            text = day.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal,
                            color = if (day.isToday) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
            val footer = listOfNotNull(
                sessions?.let { "${stringResource(Res.string.weekly_report_sessions)}: $it" },
                bestDay?.let { "${stringResource(Res.string.weekly_report_best_day)}: $it" },
            )
            if (footer.isNotEmpty()) {
                Text(
                    text = footer.joinToString("  ·  "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

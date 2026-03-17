package feature.study.ui.study

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Star
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
import androidx.compose.ui.text.font.FontWeight
import components.Pill
import components.animation.rememberAnimatedCounter
import feature.study.model.WeeklyReportUiModel
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.weekly_report_accuracy
import lexicon.resources.generated.resources.weekly_report_best_day
import lexicon.resources.generated.resources.weekly_report_cards_reviewed
import lexicon.resources.generated.resources.weekly_report_mastered
import lexicon.resources.generated.resources.weekly_report_sessions
import lexicon.resources.generated.resources.weekly_report_study_time
import lexicon.resources.generated.resources.weekly_report_title
import lexicon.resources.generated.resources.weekly_report_view_insights
import org.jetbrains.compose.resources.stringResource
import theme.AppColors
import theme.Theme

@Composable
fun WeeklyReportCard(
    report: WeeklyReportUiModel.Content,
    onViewInsights: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Theme.dimensions.cardCornerRadius),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = Theme.elevation.none,
    ) {
        Column(modifier = Modifier.padding(Theme.spacing.md)) {

            // -- Header: title + week range pill --
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.weekly_report_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (report.weekRangeLabel.isNotEmpty()) {
                    Pill(
                        text = report.weekRangeLabel,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        backgroundColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    )
                }
            }

            Spacer(Modifier.height(Theme.spacing.md))

            // -- Hero: animated card count + trend badge --
            HeroRow(
                cardsReviewed = report.cardsReviewed,
                changeLabel = report.changeLabel,
                isChangePositive = report.isChangePositive,
            )

            Spacer(Modifier.height(Theme.spacing.md))

            // -- Stats grid: 2x2 --
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xs),
            ) {
                StatItem(
                    icon = Icons.Rounded.Star,
                    label = stringResource(Res.string.weekly_report_accuracy),
                    value = report.accuracyValue,
                    tint = AppColors.secondary,
                    modifier = Modifier.weight(1f),
                )
                StatItem(
                    icon = Icons.Rounded.EmojiEvents,
                    label = stringResource(Res.string.weekly_report_mastered),
                    value = report.masteredValue,
                    tint = AppColors.tertiary,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(Theme.spacing.xs))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xs),
            ) {
                StatItem(
                    icon = Icons.Rounded.Schedule,
                    label = stringResource(Res.string.weekly_report_study_time),
                    value = report.studyTimeValue,
                    tint = AppColors.primary,
                    modifier = Modifier.weight(1f),
                )
                StatItem(
                    icon = Icons.Rounded.Insights,
                    label = stringResource(Res.string.weekly_report_sessions),
                    value = report.sessionsValue,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f),
                )
            }

            // -- Best day highlight --
            report.bestDay?.let { bestDay ->
                Spacer(Modifier.height(Theme.spacing.sm))
                BestDayRow(
                    dayName = bestDay.dayName,
                    subtitle = bestDay.subtitle,
                )
            }

            // -- CTA divider + link --
            if (report.showInsightsCta) {
                HorizontalDivider(
                    modifier = Modifier.padding(top = Theme.spacing.sm),
                    color = MaterialTheme.colorScheme.outlineVariant,
                    thickness = Theme.dimensions.hairlineThickness,
                )
                TextButton(
                    onClick = onViewInsights,
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text(
                        text = stringResource(Res.string.weekly_report_view_insights),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.primary,
                    )
                }
            }
        }
    }
}

// region Internal composables

@Composable
private fun HeroRow(
    cardsReviewed: Int,
    changeLabel: String?,
    isChangePositive: Boolean,
) {
    val animatedCards = rememberAnimatedCounter(cardsReviewed)

    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xs),
    ) {
        Text(
            text = "$animatedCards",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(Res.string.weekly_report_cards_reviewed),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = Theme.spacing.xxs),
        )

        Spacer(Modifier.weight(1f))

        if (changeLabel != null) {
            val tint = if (isChangePositive) AppColors.secondary else AppColors.error
            val icon = if (isChangePositive) {
                Icons.AutoMirrored.Rounded.TrendingUp
            } else {
                Icons.AutoMirrored.Rounded.TrendingDown
            }

            Pill(
                text = changeLabel,
                color = tint,
                backgroundAlpha = Theme.opacity.focus,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun StatItem(
    icon: ImageVector,
    label: String,
    value: String,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(Theme.shapes.small))
            .background(tint.copy(alpha = Theme.opacity.hover))
            .padding(horizontal = Theme.spacing.sm, vertical = Theme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xs),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(Theme.dimensions.iconSizeSmall),
        )
        Column {
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun BestDayRow(
    dayName: String,
    subtitle: String,
) {
    val bestDayLabel = stringResource(Res.string.weekly_report_best_day)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Theme.shapes.small))
            .background(AppColors.accentAmber.copy(alpha = Theme.opacity.hover))
            .padding(horizontal = Theme.spacing.sm, vertical = Theme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xs),
    ) {
        Icon(
            imageVector = Icons.Rounded.EmojiEvents,
            contentDescription = null,
            tint = AppColors.accentAmber,
            modifier = Modifier.size(Theme.dimensions.iconSizeMedium),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "$bestDayLabel: $dayName",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// endregion

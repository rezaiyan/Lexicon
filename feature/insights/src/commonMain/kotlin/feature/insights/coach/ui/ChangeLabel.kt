package feature.insights.coach.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import feature.insights.coach.model.ChangeUnit
import feature.insights.coach.model.StatUi
import feature.insights.coach.model.Trend
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.insights_coach_change_fewer
import lexicon.resources.generated.resources.insights_coach_change_more
import lexicon.resources.generated.resources.insights_coach_change_points_down
import lexicon.resources.generated.resources.insights_coach_change_points_up
import lexicon.resources.generated.resources.insights_coach_change_same
import org.jetbrains.compose.resources.stringResource
import theme.Theme

/** Change vs last week: arrow + amount + words, so meaning never depends on color alone. */
@Composable
internal fun ChangeLabel(stat: StatUi, modifier: Modifier = Modifier) {
    val trend = stat.trend ?: return
    val points = stat.unit == ChangeUnit.POINTS
    val text = when (trend) {
        Trend.FLAT -> stringResource(Res.string.insights_coach_change_same)
        Trend.UP -> if (points) {
            stringResource(Res.string.insights_coach_change_points_up, stat.changeAmount)
        } else {
            stringResource(Res.string.insights_coach_change_more, stat.changeAmount)
        }
        Trend.DOWN -> if (points) {
            stringResource(Res.string.insights_coach_change_points_down, stat.changeAmount)
        } else {
            stringResource(Res.string.insights_coach_change_fewer, stat.changeAmount)
        }
    }
    // Down is neutral, not red: encourage, don't scold.
    val color = when (trend) {
        Trend.UP -> Theme.colors.success
        Trend.DOWN, Trend.FLAT -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(text = text, style = MaterialTheme.typography.labelSmall, color = color, modifier = modifier)
}

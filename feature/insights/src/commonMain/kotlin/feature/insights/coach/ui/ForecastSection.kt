package feature.insights.coach.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import feature.insights.coach.model.ForecastBarUi
import feature.insights.coach.model.ForecastUi
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.insights_forecast_all_caught_up
import lexicon.resources.generated.resources.insights_forecast_busiest
import lexicon.resources.generated.resources.insights_forecast_due_now
import lexicon.resources.generated.resources.insights_forecast_review_now
import lexicon.resources.generated.resources.insights_forecast_tomorrow
import org.jetbrains.compose.resources.stringResource
import theme.Theme

private val ChartHeight = 72.dp
private val MinBarHeight = 3.dp

/** Next seven days of reviews: what's due now (with a button), tomorrow, and the busiest day. */
@Composable
internal fun ForecastSection(forecast: ForecastUi, onReviewNow: () -> Unit, showReviewNow: Boolean = true) {
    InsightsPanel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Theme.spacing.xxxs)) {
                Text(
                    text = if (forecast.dueNow > 0) {
                        stringResource(Res.string.insights_forecast_due_now, forecast.dueNow)
                    } else {
                        stringResource(Res.string.insights_forecast_all_caught_up)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = listOfNotNull(
                        stringResource(Res.string.insights_forecast_tomorrow, forecast.tomorrow),
                        forecast.busiest?.let { stringResource(Res.string.insights_forecast_busiest, it) },
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (showReviewNow && forecast.dueNow > 0) {
                Button(onClick = onReviewNow) { Text(stringResource(Res.string.insights_forecast_review_now)) }
            }
        }
        ForecastChart(forecast.bars)
    }
}

@Composable
private fun ForecastChart(bars: List<ForecastBarUi>) {
    Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.xxs)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(ChartHeight),
            horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xs),
            verticalAlignment = Alignment.Bottom,
        ) {
            bars.forEach { bar -> Bar(bar, Modifier.weight(1f)) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xs)) {
            bars.forEach { bar ->
                Text(
                    text = weekdayInitial(bar.isoDay),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (bar.isToday) FontWeight.Bold else FontWeight.Normal,
                    color = if (bar.isToday) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.weight(1f).clearAndSetSemantics { },
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun Bar(bar: ForecastBarUi, modifier: Modifier) {
    val label = weekdayInitial(bar.isoDay)
    val color = when {
        bar.isToday || bar.isBusiest -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.primary.copy(alpha = Theme.opacity.dimming)
    }
    val barShape = RoundedCornerShape(topStart = Theme.shapes.extraSmall, topEnd = Theme.shapes.extraSmall)
    Column(
        modifier = modifier
            .fillMaxHeight()
            .semantics { contentDescription = "$label ${bar.reviews}" },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
    ) {
        if (bar.reviews > 0) {
            Text(
                text = bar.reviews.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = Theme.spacing.xxxs),
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                .heightIn(min = MinBarHeight)
                .fillMaxHeight(bar.fraction * BAR_MAX_FRACTION)
                .background(color, barShape)
        )
    }
}

/** Leaves room above the tallest bar for its count label. */
private const val BAR_MAX_FRACTION = 0.8f

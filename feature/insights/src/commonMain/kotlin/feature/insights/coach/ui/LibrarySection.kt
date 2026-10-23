package feature.insights.coach.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import domain.insights.model.InsightWord
import feature.insights.coach.model.StagesUi
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.insights_coach_hardest
import lexicon.resources.generated.resources.insights_hardest_right
import lexicon.resources.generated.resources.insights_review_hardest
import lexicon.resources.generated.resources.insights_stage_learning
import lexicon.resources.generated.resources.insights_stage_mastered
import lexicon.resources.generated.resources.insights_stage_new
import lexicon.resources.generated.resources.insights_stage_strong
import org.jetbrains.compose.resources.stringResource
import theme.Theme

private val StageBarHeight = 10.dp
private val LegendDotSize = 8.dp

/** Where the library stands (new → mastered) and the words that need work most, with a way to review them. */
@Composable
internal fun LibrarySection(stages: StagesUi, hardest: List<InsightWord>, onReviewHardest: () -> Unit) {
    val buckets = listOf(
        Bucket(stringResource(Res.string.insights_stage_new), stages.new, MaterialTheme.colorScheme.outlineVariant),
        Bucket(
            stringResource(Res.string.insights_stage_learning),
            stages.learning,
            MaterialTheme.colorScheme.primary.copy(alpha = Theme.opacity.dimming),
        ),
        Bucket(stringResource(Res.string.insights_stage_strong), stages.strong, MaterialTheme.colorScheme.primary),
        Bucket(stringResource(Res.string.insights_stage_mastered), stages.mastered, Theme.colors.success),
    )
    InsightsPanel {
        StageBar(buckets, stages.total)
        Row(Modifier.fillMaxWidth()) {
            buckets.forEach { LegendItem(it, Modifier.weight(1f)) }
        }
        if (hardest.isNotEmpty()) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = Theme.opacity.dimming))
            Text(
                text = stringResource(Res.string.insights_coach_hardest),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            hardest.forEach { HardWordRow(it) }
            OutlinedButton(onClick = onReviewHardest) {
                Text(stringResource(Res.string.insights_review_hardest, hardest.size))
            }
        }
    }
}

private class Bucket(val label: String, val count: Int, val color: Color)

@Composable
private fun StageBar(buckets: List<Bucket>, total: Int) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(StageBarHeight)
            .clip(RoundedCornerShape(Theme.shapes.pill))
            .semantics { contentDescription = buckets.joinToString { "${it.label} ${it.count}" } },
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xxxs),
    ) {
        buckets.filter { it.count > 0 }.forEach { bucket ->
            Box(
                Modifier
                    .weight(bucket.count.toFloat() / total)
                    .fillMaxHeight()
                    .background(bucket.color)
            )
        }
    }
}

@Composable
private fun LegendItem(bucket: Bucket, modifier: Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Theme.spacing.xxxs)) {
        Text(
            text = bucket.count.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xxs),
        ) {
            Box(Modifier.size(LegendDotSize).background(bucket.color, CircleShape))
            Text(
                text = bucket.label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun HardWordRow(word: InsightWord) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = word.text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = stringResource(Res.string.insights_hardest_right, word.accuracyPct),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

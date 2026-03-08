package feature.study.ui.review

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import components.animation.rememberAnimatedCounter
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.completion_cards_reviewed
import lexicon.resources.generated.resources.completion_forgot
import lexicon.resources.generated.resources.completion_remembered
import org.jetbrains.compose.resources.stringResource
import theme.Theme

@Composable
internal fun StatsSection(
    knownCount: Int,
    unknownCount: Int,
    totalCount: Int,
    modifier: Modifier = Modifier,
) {
    val animatedKnown = rememberAnimatedCounter(knownCount)
    val animatedUnknown = rememberAnimatedCounter(unknownCount)

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.md),
    ) {
        // Total cards label
        Text(
            text = stringResource(Res.string.completion_cards_reviewed, totalCount),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // Proportional bar
        ProportionalBar(
            knownCount = knownCount,
            unknownCount = unknownCount,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Theme.spacing.md),
        )

        Spacer(Modifier.height(Theme.spacing.xxs))

        // Stat cards row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Theme.spacing.sm),
        ) {
            StatCard(
                count = animatedKnown,
                label = stringResource(Res.string.completion_remembered),
                accentColor = Theme.colors.success,
                modifier = Modifier.weight(1f),
            )
            StatCard(
                count = animatedUnknown,
                label = stringResource(Res.string.completion_forgot),
                accentColor = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ProportionalBar(
    knownCount: Int,
    unknownCount: Int,
    modifier: Modifier = Modifier,
) {
    val total = knownCount + unknownCount
    if (total == 0) return

    val knownFraction = knownCount.toFloat() / total
    val successColor = Theme.colors.success
    val errorColor = MaterialTheme.colorScheme.error

    Row(
        modifier = modifier
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp)),
    ) {
        if (knownCount > 0) {
            Box(
                modifier = Modifier
                    .weight(knownFraction)
                    .height(8.dp)
                    .background(successColor),
            )
        }
        if (unknownCount > 0) {
            Box(
                modifier = Modifier
                    .weight(1f - knownFraction)
                    .height(8.dp)
                    .background(errorColor),
            )
        }
    }
}

@Composable
private fun StatCard(
    count: Int,
    label: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(Theme.shapes.medium),
        color = Theme.colors.surfaceContainerLow,
        tonalElevation = Theme.elevation.low,
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = Theme.spacing.md,
                vertical = Theme.spacing.sm,
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xs),
        ) {
            // Color accent bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(32.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accentColor),
            )

            Column {
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.titleLarge,
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
}

package feature.study.ui.study

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import components.SectionHeader
import components.animation.staggeredFadeSlide
import domain.word.model.LearningStage
import domain.word.model.ProgressStats
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.hide_empty_stages
import lexicon.resources.generated.resources.learning_stages
import lexicon.resources.generated.resources.show_empty_stages
import lexicon.resources.generated.resources.total_words_count
import org.jetbrains.compose.resources.stringResource
import theme.Theme

private const val EXPANDED_CHEVRON_ROTATION = 180f

/**
 * Distribution bar plus one card per stage. Stages holding no words are folded behind a
 * "Show N empty stages" toggle so a young library isn't a wall of zero cards.
 */
@Composable
fun LearningStagesSection(
    stats: ProgressStats,
    onStageClick: (LearningStage, String) -> Unit,
    onStageLongClick: ((LearningStage, String) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    var showEmptyStages by rememberSaveable { mutableStateOf(false) }
    val emptyStageCount = LearningStage.entries.count { stats.countFor(it) == 0 }

    Column(
        modifier = modifier
            .padding(top = Theme.spacing.sectionGap)
            .animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.sectionHeaderGap),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .staggeredFadeSlide(index = 0, baseDelayMs = 0),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionHeader(title = stringResource(Res.string.learning_stages))
            if (stats.totalWords > 0) {
                Text(
                    text = stringResource(Res.string.total_words_count, stats.totalWords),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        WordDistributionBar(stats = stats)

        LearningStagesList(
            stats = stats,
            onStageClick = onStageClick,
            onStageLongClick = onStageLongClick,
            showEmptyStages = showEmptyStages,
        )

        if (emptyStageCount > 0) {
            EmptyStagesToggle(
                expanded = showEmptyStages,
                emptyStageCount = emptyStageCount,
                onToggle = { showEmptyStages = !showEmptyStages },
            )
        }
    }
}

@Composable
private fun EmptyStagesToggle(
    expanded: Boolean,
    emptyStageCount: Int,
    onToggle: () -> Unit,
) {
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) EXPANDED_CHEVRON_ROTATION else 0f,
        label = "emptyStagesChevron",
    )
    TextButton(
        onClick = onToggle,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = if (expanded) {
                stringResource(Res.string.hide_empty_stages)
            } else {
                stringResource(Res.string.show_empty_stages, emptyStageCount)
            },
            style = MaterialTheme.typography.labelLarge,
        )
        Icon(
            imageVector = Icons.Rounded.KeyboardArrowDown,
            contentDescription = null,
            modifier = Modifier
                .padding(start = Theme.spacing.xxs)
                .size(Theme.dimensions.iconSizeMedium)
                .rotate(chevronRotation),
        )
    }
}

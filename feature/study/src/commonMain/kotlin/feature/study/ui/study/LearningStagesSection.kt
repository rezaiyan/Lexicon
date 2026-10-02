package feature.study.ui.study

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import components.SectionHeader
import components.animation.staggeredFadeSlide
import domain.word.model.LearningStage
import domain.word.model.ProgressStats
import org.jetbrains.compose.resources.stringResource
import theme.Theme
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.learning_stages
import lexicon.resources.generated.resources.total_words_count

@Composable
fun LearningStagesSection(
    stats: ProgressStats,
    onStageClick: (LearningStage, String) -> Unit,
    onStageLongClick: ((LearningStage, String) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(top = Theme.spacing.sectionGap),
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
        )
    }
}

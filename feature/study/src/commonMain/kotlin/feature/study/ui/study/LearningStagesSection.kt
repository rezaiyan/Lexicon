package feature.study.ui.study

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import components.SectionHeader
import components.animation.staggeredFadeSlide
import domain.word.model.LearningStage
import domain.word.model.ProgressStats
import org.jetbrains.compose.resources.stringResource
import theme.Theme
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.learning_stages

@Composable
fun LearningStagesSection(
    stats: ProgressStats,
    onStageClick: (LearningStage, String) -> Unit,
    onStageLongClick: ((LearningStage, String) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        SectionHeader(
            title = stringResource(Res.string.learning_stages),
            modifier = Modifier
                .staggeredFadeSlide(index = 0, baseDelayMs = 0)
                .padding(vertical = Theme.spacing.md)
        )

        LearningStagesList(
            stats = stats,
            onStageClick = onStageClick,
            onStageLongClick = onStageLongClick,
        )
    }
}

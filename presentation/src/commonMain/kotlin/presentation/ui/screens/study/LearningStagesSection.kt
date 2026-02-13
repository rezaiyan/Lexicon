package presentation.ui.screens.study

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            stringResource(Res.string.learning_stages),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = Theme.spacing.cardSpacingLarge)
        )
        
        LearningStagesList(
            stats = stats,
            onStageClick = onStageClick
        )
    }
}


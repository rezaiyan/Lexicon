package presentation.ui.screens.study

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import domain.word.model.LearningStage
import domain.word.model.ProgressStats
import theme.LexiconTheme

data class LearningStagesListPreviewParam(
    val stats: ProgressStats
)

class LearningStagesListPreviewProvider : PreviewParameterProvider<LearningStagesListPreviewParam> {
    override val values: Sequence<LearningStagesListPreviewParam>
        get() = sequenceOf(
            LearningStagesListPreviewParam(
                stats = ProgressStats(
                    level0Count = 10,
                    level1Count = 5,
                    level2Count = 8,
                    level3Count = 12,
                    level4Count = 15,
                    level5Count = 20,
                    level6Count = 30,
                    totalWords = 100,
                    dueCards = 5
                )
            ),
            LearningStagesListPreviewParam(
                stats = ProgressStats(
                    level0Count = 0,
                    level1Count = 0,
                    level2Count = 0,
                    level3Count = 0,
                    level4Count = 0,
                    level5Count = 0,
                    level6Count = 0,
                    totalWords = 0,
                    dueCards = 0
                )
            ),
            LearningStagesListPreviewParam(
                stats = ProgressStats(
                    level0Count = 50,
                    level1Count = 30,
                    level2Count = 25,
                    level3Count = 20,
                    level4Count = 15,
                    level5Count = 10,
                    level6Count = 5,
                    totalWords = 155,
                    dueCards = 12
                )
            ),
            LearningStagesListPreviewParam(
                stats = ProgressStats(
                    level0Count = 100,
                    level1Count = 0,
                    level2Count = 0,
                    level3Count = 0,
                    level4Count = 0,
                    level5Count = 0,
                    level6Count = 0,
                    totalWords = 100,
                    dueCards = 100
                )
            )
        )
}

@Preview(showBackground = true, name = "Learning Stages - With Stats")
@Composable
private fun LearningStagesListPreviewWithStats() {
    LexiconTheme {
        LearningStagesList(
            stats = ProgressStats(
                level0Count = 234234,
                level1Count = 5,
                level2Count = 8,
                level3Count = 12,
                level4Count = 15,
                level5Count = 20,
                level6Count = 30,
                totalWords = 100,
                dueCards = 5
            ),
            onStageClick = { _: LearningStage, _: String -> },
            levelTexts = listOf("Fresh Fresh Fresh Fresh Fresh", "Learning", "Familiar", "Building", "Almost", "Strong", "Mastered"),
            levelNames = listOf("Fresh", "Learning", "Familiar", "Building", "Almost", "Strong", "Mastered"),
            levelDescriptions = listOf(
                "Brand new words words words words words",
                "First learning phase",
                "Getting familiar",
                "Building confidence",
                "Almost there",
                "Strong grasp",
                "Fully mastered"
            )
        )
    }
}

@Preview(showBackground = true, name = "Learning Stages - Empty")
@Composable
private fun LearningStagesListPreviewEmpty() {
    LexiconTheme {
        LearningStagesList(
            stats = ProgressStats(
                level0Count = 0,
                level1Count = 0,
                level2Count = 0,
                level3Count = 0,
                level4Count = 0,
                level5Count = 0,
                level6Count = 0,
                totalWords = 0,
                dueCards = 0
            ),
            onStageClick = { _: LearningStage, _: String -> },
            levelTexts = listOf("Fresh", "Learning", "Familiar", "Building", "Almost", "Strong", "Mastered"),
            levelNames = listOf("Fresh", "Learning", "Familiar", "Building", "Almost", "Strong", "Mastered"),
            levelDescriptions = listOf(
                "Brand new words",
                "First learning phase",
                "Getting familiar",
                "Building confidence",
                "Almost there",
                "Strong grasp",
                "Fully mastered"
            )
        )
    }
}

@Preview(showBackground = true, name = "Learning Stages - Preview Parameter")
@Composable
private fun LearningStagesListPreviewWithParams(
    @PreviewParameter(LearningStagesListPreviewProvider::class)
    previewParam: LearningStagesListPreviewParam
) {
    LexiconTheme {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)){
            LearningStagesList(
                stats = previewParam.stats,
                onStageClick = { _: LearningStage, _: String -> },
                levelTexts = listOf("Fresh", "Learning", "Familiar", "Building", "Almost", "Strong", "Mastered"),
                levelNames = listOf("Fresh", "Learning", "Familiar", "Building", "Almost", "Strong", "Mastered"),
                levelDescriptions = listOf(
                    "Brand new words",
                    "First learning phase",
                    "Getting familiar",
                    "Building confidence",
                    "Almost there",
                    "Strong grasp",
                    "Fully mastered"
                )
            )
        }
    }
}


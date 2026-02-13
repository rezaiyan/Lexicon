@file:OptIn(InternalResourceApi::class)

package presentation.ui.screens.study

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import domain.word.model.LearningStage
import domain.word.model.ProgressStats
import org.jetbrains.compose.resources.InternalResourceApi
import org.jetbrains.compose.resources.stringResource
import presentation.ui.components.LevelBucketCard
import theme.AppColors
import theme.Theme
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.level_0_description
import lexicon.resources.generated.resources.level_0_fresh
import lexicon.resources.generated.resources.level_1_description
import lexicon.resources.generated.resources.level_1_learning
import lexicon.resources.generated.resources.level_2_description
import lexicon.resources.generated.resources.level_2_familiar
import lexicon.resources.generated.resources.level_3_building
import lexicon.resources.generated.resources.level_3_description
import lexicon.resources.generated.resources.level_4_almost
import lexicon.resources.generated.resources.level_4_description
import lexicon.resources.generated.resources.level_5_description
import lexicon.resources.generated.resources.level_5_strong
import lexicon.resources.generated.resources.level_6_description
import lexicon.resources.generated.resources.level_6_mastered

data class LevelBucketData(
    val stage: LearningStage,
    val nameResId: org.jetbrains.compose.resources.StringResource,
    val descriptionResId: org.jetbrains.compose.resources.StringResource,
    val icon: String,
    val color: androidx.compose.ui.graphics.Color,
    val count: Int
)

@Composable
fun LearningStagesList(
    stats: ProgressStats,
    onStageClick: (LearningStage, String) -> Unit,
    levelTexts: List<String>? = null,
    levelNames: List<String>? = null,
    levelDescriptions: List<String>? = null
) {
    // Level texts for onClick callbacks
    val level0Text = levelTexts?.getOrNull(0) ?: stringResource(Res.string.level_0_fresh)
    val level1Text = levelTexts?.getOrNull(1) ?: stringResource(Res.string.level_1_learning)
    val level2Text = levelTexts?.getOrNull(2) ?: stringResource(Res.string.level_2_familiar)
    val level3Text = levelTexts?.getOrNull(3) ?: stringResource(Res.string.level_3_building)
    val level4Text = levelTexts?.getOrNull(4) ?: stringResource(Res.string.level_4_almost)
    val level5Text = levelTexts?.getOrNull(5) ?: stringResource(Res.string.level_5_strong)
    val level6Text = levelTexts?.getOrNull(6) ?: stringResource(Res.string.level_6_mastered)

    val levels = listOf(
        LevelBucketData(
            LearningStage.LEVEL_0_FRESH,
            Res.string.level_0_fresh,
            Res.string.level_0_description,
            "📝",
            AppColors.novice,
            stats.level0Count
        ),
        LevelBucketData(
            LearningStage.LEVEL_1_LEARNING,
            Res.string.level_1_learning,
            Res.string.level_1_description,
            "📚",
            AppColors.apprentice,
            stats.level1Count
        ),
        LevelBucketData(
            LearningStage.LEVEL_2_FAMILIAR,
            Res.string.level_2_familiar,
            Res.string.level_2_description,
            "💡",
            AppColors.apprentice,
            stats.level2Count
        ),
        LevelBucketData(
            LearningStage.LEVEL_3_BUILDING,
            Res.string.level_3_building,
            Res.string.level_3_description,
            "✨",
            AppColors.adept,
            stats.level3Count
        ),
        LevelBucketData(
            LearningStage.LEVEL_4_ALMOST,
            Res.string.level_4_almost,
            Res.string.level_4_description,
            "🌱",
            AppColors.adept,
            stats.level4Count
        ),
        LevelBucketData(
            LearningStage.LEVEL_5_STRONG,
            Res.string.level_5_strong,
            Res.string.level_5_description,
            "🌟",
            AppColors.master,
            stats.level5Count
        ),
        LevelBucketData(
            LearningStage.LEVEL_6_MASTERED,
            Res.string.level_6_mastered,
            Res.string.level_6_description,
            "👑",
            AppColors.master,
            stats.level6Count
        )
    )

    val levelTexts =
        listOf(level0Text, level1Text, level2Text, level3Text, level4Text, level5Text, level6Text)

    Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.cardSpacing)) {
        levels.forEachIndexed { index, level ->
            LevelBucketCard(
                level = levelNames?.getOrNull(index) ?: stringResource(level.nameResId),
                description = levelDescriptions?.getOrNull(index) ?: stringResource(level.descriptionResId),
                count = level.count,
                color = level.color,
                icon = level.icon,
                onClick = {
                    if (level.count > 0) {
                        onStageClick(level.stage, levelTexts[index])
                    }
                }
            )
        }
    }
}



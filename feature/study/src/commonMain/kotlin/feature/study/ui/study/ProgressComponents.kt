package feature.study.ui.study

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import domain.word.model.LearningStage
import domain.word.model.ProgressStats
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import components.animation.staggeredFadeSlide
import feature.study.ui.components.LevelBucketCard
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

private const val STAGE_DESCRIPTION_SEPARATOR = "•"

private data class StageStyle(
    val stage: LearningStage,
    val nameResId: StringResource,
    val descriptionResId: StringResource,
    val icon: ImageVector,
    val color: Color,
)

private val StageStyles = listOf(
    StageStyle(
        LearningStage.LEVEL_0_FRESH,
        Res.string.level_0_fresh,
        Res.string.level_0_description,
        Icons.Rounded.Lightbulb,
        AppColors.novice,
    ),
    StageStyle(
        LearningStage.LEVEL_1_LEARNING,
        Res.string.level_1_learning,
        Res.string.level_1_description,
        Icons.Rounded.MenuBook,
        AppColors.apprentice,
    ),
    StageStyle(
        LearningStage.LEVEL_2_FAMILIAR,
        Res.string.level_2_familiar,
        Res.string.level_2_description,
        Icons.Rounded.AutoAwesome,
        AppColors.apprentice,
    ),
    StageStyle(
        LearningStage.LEVEL_3_BUILDING,
        Res.string.level_3_building,
        Res.string.level_3_description,
        Icons.Rounded.TrendingUp,
        AppColors.adept,
    ),
    StageStyle(
        LearningStage.LEVEL_4_ALMOST,
        Res.string.level_4_almost,
        Res.string.level_4_description,
        Icons.Rounded.Verified,
        AppColors.adept,
    ),
    StageStyle(
        LearningStage.LEVEL_5_STRONG,
        Res.string.level_5_strong,
        Res.string.level_5_description,
        Icons.Rounded.Star,
        AppColors.master,
    ),
    StageStyle(
        LearningStage.LEVEL_6_MASTERED,
        Res.string.level_6_mastered,
        Res.string.level_6_description,
        Icons.Rounded.EmojiEvents,
        AppColors.master,
    ),
)

/**
 * One card per learning stage. With [showEmptyStages] off, stages holding no words are left
 * out so the list only shows where the user's words actually are.
 */
@Composable
fun LearningStagesList(
    stats: ProgressStats,
    onStageClick: (LearningStage, String) -> Unit,
    onStageLongClick: ((LearningStage, String) -> Unit)? = null,
    showEmptyStages: Boolean = true,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.listGap)) {
        StageStyles.forEachIndexed { index, style ->
            val count = stats.countFor(style.stage)
            if (count == 0 && !showEmptyStages) return@forEachIndexed
            key(style.stage) {
                val name = stringResource(style.nameResId)
                // Descriptions read "Level 1 • Just discovered" in every locale: split into overline + subtitle
                val fullDescription = stringResource(style.descriptionResId)
                val hasOverline = fullDescription.contains(STAGE_DESCRIPTION_SEPARATOR)
                LevelBucketCard(
                    modifier = Modifier.staggeredFadeSlide(index + 1),
                    level = name,
                    overline = if (hasOverline) {
                        fullDescription.substringBefore(STAGE_DESCRIPTION_SEPARATOR).trim().uppercase()
                    } else {
                        null
                    },
                    description = if (hasOverline) {
                        fullDescription.substringAfter(STAGE_DESCRIPTION_SEPARATOR).trim()
                    } else {
                        fullDescription
                    },
                    count = count,
                    color = style.color,
                    icon = style.icon,
                    onClick = { if (count > 0) onStageClick(style.stage, name) },
                    onLongClick = if (count > 0 && onStageLongClick != null) {
                        { onStageLongClick(style.stage, name) }
                    } else {
                        null
                    },
                )
            }
        }
    }
}

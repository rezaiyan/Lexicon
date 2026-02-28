package presentation.ui.screens.study

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import domain.word.model.ProgressEvaluation
import domain.word.model.ProgressTier
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.almost_a_master
import lexicon.resources.generated.resources.almost_a_master_subtitle
import lexicon.resources.generated.resources.building_foundation
import lexicon.resources.generated.resources.building_foundation_subtitle
import lexicon.resources.generated.resources.fully_mastered
import lexicon.resources.generated.resources.fully_mastered_subtitle
import lexicon.resources.generated.resources.getting_started
import lexicon.resources.generated.resources.getting_started_subtitle
import lexicon.resources.generated.resources.import_words
import lexicon.resources.generated.resources.lets_go
import lexicon.resources.generated.resources.making_great_progress
import lexicon.resources.generated.resources.over_halfway
import lexicon.resources.generated.resources.over_halfway_subtitle
import lexicon.resources.generated.resources.progress_subtitle
import lexicon.resources.generated.resources.ready_to_learn
import lexicon.resources.generated.resources.ready_to_learn_subtitle
import lexicon.resources.generated.resources.start_review
import lexicon.resources.generated.resources.strong_knowledge
import lexicon.resources.generated.resources.strong_knowledge_subtitle
import org.jetbrains.compose.resources.stringResource
import theme.AppColors
import theme.Theme

@Composable
fun StatsSection(
    evaluation: ProgressEvaluation,
    dueCards: Int,
    onImportWords: () -> Unit,
    onStartReview: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = when (evaluation.tier) {
        ProgressTier.EMPTY,
        ProgressTier.ALMOST_MASTER,
        ProgressTier.MASTERED -> AppColors.master

        else -> AppColors.secondary
    }

    val trackColor = MaterialTheme.colorScheme.outlineVariant

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Theme.shapes.large),
        colors = CardDefaults.cardColors(
            containerColor = AppColors.primary.copy(alpha = 0.06f)
        )
    ) {
        Box {
            // Decorative blob in top-right corner
            Canvas(modifier = Modifier.matchParentSize()) {
                drawCircle(
                    color = AppColors.primary.copy(alpha = 0.06f),
                    radius = 100.dp.toPx(),
                    center = Offset(
                        x = size.width - 30.dp.toPx(),
                        y = 10.dp.toPx()
                    )
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Theme.spacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Progress ring
                ProgressRing(
                    progress = evaluation.progressFraction,
                    progressColor = accentColor,
                    modifier = Modifier.size(110.dp),
                    trackColor = trackColor,
                ) {
                    Text(
                        text = if (evaluation.tier == ProgressTier.EMPTY) {
                            stringResource(Res.string.lets_go)
                        } else {
                            "${evaluation.progressPercent}%"
                        },
                        style = if (evaluation.tier == ProgressTier.EMPTY) {
                            MaterialTheme.typography.titleMedium
                        } else {
                            MaterialTheme.typography.headlineSmall
                        },
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(Modifier.width(Theme.spacing.md))

                // Text + button column
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tierTitle(evaluation.tier),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )

                    Spacer(Modifier.height(Theme.spacing.xxs))

                    Text(
                        text = tierSubtitle(evaluation.tier),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val isEmpty = evaluation.tier == ProgressTier.EMPTY
                    val hasDueCards = dueCards > 0

                    if (isEmpty || hasDueCards) {
                        Spacer(Modifier.height(Theme.spacing.sm))

                        Button(
                            onClick = if (isEmpty) onImportWords else onStartReview,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(Theme.shapes.pill),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AppColors.primary,
                                contentColor = Color.White
                            )
                        ) {
                            Text(
                                text = if (isEmpty) {
                                    stringResource(Res.string.import_words)
                                } else {
                                    stringResource(Res.string.start_review)
                                },
                            maxLines = 1,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            autoSize = TextAutoSize.StepBased(
                                minFontSize = 10.sp,
                                maxFontSize = MaterialTheme.typography.labelMedium.fontSize,
                                stepSize = 1.sp
                            )
                        )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun tierTitle(tier: ProgressTier): String = when (tier) {
    ProgressTier.EMPTY -> stringResource(Res.string.ready_to_learn)
    ProgressTier.GETTING_STARTED -> stringResource(Res.string.getting_started)
    ProgressTier.BUILDING -> stringResource(Res.string.building_foundation)
    ProgressTier.PROGRESSING -> stringResource(Res.string.making_great_progress)
    ProgressTier.HALFWAY -> stringResource(Res.string.over_halfway)
    ProgressTier.STRONG -> stringResource(Res.string.strong_knowledge)
    ProgressTier.ALMOST_MASTER -> stringResource(Res.string.almost_a_master)
    ProgressTier.MASTERED -> stringResource(Res.string.fully_mastered)
}

@Composable
private fun tierSubtitle(tier: ProgressTier): String = when (tier) {
    ProgressTier.EMPTY -> stringResource(Res.string.ready_to_learn_subtitle)
    ProgressTier.GETTING_STARTED -> stringResource(Res.string.getting_started_subtitle)
    ProgressTier.BUILDING -> stringResource(Res.string.building_foundation_subtitle)
    ProgressTier.PROGRESSING -> stringResource(Res.string.progress_subtitle)
    ProgressTier.HALFWAY -> stringResource(Res.string.over_halfway_subtitle)
    ProgressTier.STRONG -> stringResource(Res.string.strong_knowledge_subtitle)
    ProgressTier.ALMOST_MASTER -> stringResource(Res.string.almost_a_master_subtitle)
    ProgressTier.MASTERED -> stringResource(Res.string.fully_mastered_subtitle)
}

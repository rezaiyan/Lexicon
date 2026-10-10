package feature.study.ui.wordrush

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import components.LottieMotionIcon
import components.animation.ConfettiOverlay
import domain.wordrush.model.WordRushGrade
import domain.wordrush.model.WordRushMiss
import feature.study.wordrush.WordRushPhase
import feature.study.wordrush.WordRushViewModel
import kotlinx.coroutines.launch
import utils.LexiconFormatters
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.close
import lexicon.resources.generated.resources.word_rush_accuracy
import lexicon.resources.generated.resources.word_rush_avg_speed
import lexicon.resources.generated.resources.word_rush_correct_label
import lexicon.resources.generated.resources.word_rush_best_streak_result
import lexicon.resources.generated.resources.word_rush_game_over
import lexicon.resources.generated.resources.word_rush_grade_a
import lexicon.resources.generated.resources.word_rush_grade_b
import lexicon.resources.generated.resources.word_rush_grade_c
import lexicon.resources.generated.resources.word_rush_grade_d
import lexicon.resources.generated.resources.word_rush_grade_s
import lexicon.resources.generated.resources.word_rush_new_best
import lexicon.resources.generated.resources.word_rush_out_of_lives
import lexicon.resources.generated.resources.word_rush_perfect_round
import lexicon.resources.generated.resources.word_rush_play_again
import lexicon.resources.generated.resources.word_rush_points
import lexicon.resources.generated.resources.word_rush_result_score
import lexicon.resources.generated.resources.word_rush_review_timed_out
import lexicon.resources.generated.resources.word_rush_review_title
import lexicon.resources.generated.resources.word_rush_review_you_picked
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import theme.AppColors
import theme.Theme

private const val CONFETTI_BEST_RESULT_LOTTIE_URL =
    "https://assets-v2.lottiefiles.com/a/76caddfc-116a-11ee-aa25-6333db7b8d8c/GpI0uQYnKa.lottie"

private const val THUMBS_UP_LOTTIE_URL =
    "https://assets-v2.lottiefiles.com/a/7b075652-1183-11ee-a203-53906fc94c50/XDST3mJaJS.lottie"

private const val REJECT_WORST_RESULT_LOTTIE_URL =
    "https://assets-v2.lottiefiles.com/a/6c3d5f74-1177-11ee-9eab-379152912a9b/OA41748MGh.json"

private enum class ResultTier { Excellent, Average, Poor }

private data class GradeStyle(
    val color: Color,
    val messageRes: StringResource,
    val tier: ResultTier,
)

private fun gradeStyleOf(grade: WordRushGrade): GradeStyle = when (grade) {
    WordRushGrade.S -> GradeStyle(AppColors.accentAmber, Res.string.word_rush_grade_s, ResultTier.Excellent)
    WordRushGrade.A -> GradeStyle(AppColors.secondary,   Res.string.word_rush_grade_a, ResultTier.Excellent)
    WordRushGrade.B -> GradeStyle(AppColors.primary,     Res.string.word_rush_grade_b, ResultTier.Average)
    WordRushGrade.C -> GradeStyle(AppColors.tertiary,    Res.string.word_rush_grade_c, ResultTier.Average)
    WordRushGrade.D -> GradeStyle(AppColors.error,       Res.string.word_rush_grade_d, ResultTier.Poor)
}

@Composable
internal fun ResultContent(
    phase: WordRushPhase.Result,
    onPlayAgain: () -> Unit,
    onDismiss: () -> Unit,
) {
    val style = gradeStyleOf(phase.grade)

    Box(modifier = Modifier.fillMaxSize()) {
        // Summary and actions always fit on one screen; only the missed-words list
        // (unbounded length) takes the leftover height and scrolls inside its card.
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ResultGradeBadge(grade = phase.grade, style = style)
                ResultReactionAnimation(tier = style.tier)
            }
            Spacer(Modifier.height(Theme.spacing.sm))
            Text(
                text = stringResource(
                    if (phase.endedByLives) Res.string.word_rush_out_of_lives else Res.string.word_rush_game_over,
                ),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(style.messageRes),
                style = MaterialTheme.typography.titleMedium,
                color = style.color,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
            )
            ResultNewBestBadge(isNewBest = phase.isNewBest)
            Spacer(Modifier.height(Theme.spacing.md))
            ResultStatsRow(phase = phase)
            Spacer(Modifier.height(Theme.spacing.sm))
            Row(
                horizontalArrangement = Arrangement.spacedBy(Theme.spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ResultLivesRow(livesRemaining = phase.livesRemaining)
                ResultStreakRow(bestStreak = phase.bestStreak)
            }
            Spacer(Modifier.height(Theme.spacing.md))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.TopCenter,
            ) {
                ResultMissedWords(
                    misses = phase.missedWords,
                    showPerfect = phase.missedWords.isEmpty() && phase.answeredCount > 0,
                )
            }
            Spacer(Modifier.height(Theme.spacing.md))
            ResultActions(onPlayAgain = onPlayAgain, onDismiss = onDismiss)
        }

        // Confetti-only overlay: decorative particles that don't block any UI elements
        ResultConfettiOverlay(isNewBest = phase.isNewBest, tier = style.tier)
    }
}

@Composable
private fun ResultGradeBadge(grade: WordRushGrade, style: GradeStyle) {
    Box(contentAlignment = Alignment.Center) {
        if (style.tier == ResultTier.Excellent) {
            ExcellentGlowRing(color = style.color)
        }
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(color = style.color.copy(alpha = 0.15f), shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = grade.code,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = style.color,
            )
        }
    }
}

@Composable
private fun ExcellentGlowRing(color: Color) {
    val transition = rememberInfiniteTransition(label = "grade-ring")
    val rotation = transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "grade-ring-rotation",
    )
    Box(
        modifier = Modifier
            .size(96.dp)
            .drawBehind {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color.Transparent,
                            color.copy(alpha = 0.9f),
                            color,
                            color.copy(alpha = 0.9f),
                            Color.Transparent,
                        ),
                    ),
                    startAngle = rotation.value,
                    sweepAngle = 270f,
                    useCenter = false,
                    style = Stroke(width = 3.dp.toPx()),
                )
            },
    )
}

@Composable
private fun ResultLivesRow(livesRemaining: Int) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(WordRushViewModel.INITIAL_LIVES) { index ->
            val isAlive = index < livesRemaining
            Icon(
                imageVector = if (isAlive) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = null,
                tint = if (isAlive) AppColors.error else MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer { alpha = if (isAlive) 1f else 0.35f },
            )
            if (index < WordRushViewModel.INITIAL_LIVES - 1) Spacer(Modifier.width(4.dp))
        }
    }
}

@Composable
private fun ResultStatsRow(phase: WordRushPhase.Result) {
    val scoreAnim = remember { Animatable(0f) }
    val accuracyAnim = remember { Animatable(0f) }
    val speedAnim = remember { Animatable(0f) }
    val correctAnim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch { scoreAnim.animateTo(phase.score.toFloat(), tween(1200, easing = LinearEasing)) }
        launch { correctAnim.animateTo(phase.correctCount.toFloat(), tween(1200, easing = LinearEasing)) }
        launch { accuracyAnim.animateTo(phase.accuracy, tween(1200, easing = LinearEasing)) }
        speedAnim.animateTo(phase.avgResponseTimeMs.toFloat(), tween(1200, easing = LinearEasing))
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        ResultStatColumn(
            value = scoreAnim.value.toInt().toString(),
            label = stringResource(Res.string.word_rush_points),
            color = AppColors.primary,
        )
        ResultStatColumn(
            value = stringResource(
                Res.string.word_rush_result_score,
                correctAnim.value.toInt(),
                phase.totalQuestions,
            ),
            label = stringResource(Res.string.word_rush_correct_label),
            color = AppColors.accentAmber,
        )
        ResultStatColumn(
            value = "${(accuracyAnim.value * 100).toInt()}%",
            label = stringResource(Res.string.word_rush_accuracy),
            color = AppColors.secondary,
        )
        ResultStatColumn(
            value = LexiconFormatters.secondsOneDecimal(speedAnim.value.toLong()),
            label = stringResource(Res.string.word_rush_avg_speed),
            color = AppColors.tertiary,
        )
    }
}

@Composable
private fun ResultStreakRow(bestStreak: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        val fireCount = bestStreak.coerceAtMost(3)
        if (fireCount > 0) {
            repeat(fireCount) {
                Icon(
                    imageVector = Icons.Rounded.LocalFireDepartment,
                    contentDescription = null,
                    tint = AppColors.tertiary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(Theme.spacing.xs))
        }
        Text(
            text = stringResource(Res.string.word_rush_best_streak_result, bestStreak),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ResultNewBestBadge(isNewBest: Boolean) {
    val motion = Theme.motion
    AnimatedVisibility(
        visible = isNewBest,
        enter = fadeIn(tween(motion.durationShort2)) + scaleIn(
            initialScale = 0.7f,
            animationSpec = spring(dampingRatio = 0.5f),
        ),
        label = "new-best",
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(Theme.spacing.xs))
            Box(
                modifier = Modifier
                    .border(
                        width = 1.5.dp,
                        brush = Brush.linearGradient(
                            listOf(AppColors.leaderboardGold, AppColors.accentAmber, AppColors.leaderboardGold),
                        ),
                        shape = RoundedCornerShape(Theme.shapes.medium),
                    )
                    .background(
                        color = AppColors.leaderboardGold.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(Theme.shapes.medium),
                    )
                    .padding(horizontal = Theme.spacing.md, vertical = Theme.spacing.xs),
            ) {
                Text(
                    text = stringResource(Res.string.word_rush_new_best),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.leaderboardGold,
                )
            }
        }
    }
}

@Composable
private fun ResultActions(onPlayAgain: () -> Unit, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.sm),
    ) {
        Button(
            onClick = onDismiss,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
            shape = RoundedCornerShape(Theme.shapes.pill),
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = stringResource(Res.string.close),
                maxLines = 1,
                modifier = Modifier.padding(vertical = Theme.spacing.xs),
            )
        }
        Button(
            onClick = onPlayAgain,
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.tertiary),
            shape = RoundedCornerShape(Theme.shapes.pill),
            modifier = Modifier.weight(1.5f),
        ) {
            Text(
                text = stringResource(Res.string.word_rush_play_again),
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                modifier = Modifier.padding(vertical = Theme.spacing.xs),
            )
        }
    }
}

/**
 * Reaction animation shown inline in the layout (takes space, never overlaps stats below).
 * Excellent tier is celebrated by confetti particles — no inline animation needed.
 */
@Composable
private fun ResultReactionAnimation(tier: ResultTier) {
    when (tier) {
        ResultTier.Excellent -> Unit
        ResultTier.Average -> LottieMotionIcon(
            url = THUMBS_UP_LOTTIE_URL,
            modifier = Modifier.size(64.dp),
            iterations = 1,
        )
        ResultTier.Poor -> LottieMotionIcon(
            url = REJECT_WORST_RESULT_LOTTIE_URL,
            modifier = Modifier.size(64.dp),
            iterations = 1,
        )
    }
}

/**
 * Confetti-only overlay. Particle/full-screen animations that are purely decorative:
 * they draw over content but don't visually block legibility or intercept touch events.
 */
@Composable
private fun ResultConfettiOverlay(isNewBest: Boolean, tier: ResultTier) {
    when {
        isNewBest -> LottieMotionIcon(
            url = CONFETTI_BEST_RESULT_LOTTIE_URL,
            modifier = Modifier.fillMaxSize(),
            iterations = 1,
        )
        tier == ResultTier.Excellent -> ConfettiOverlay(modifier = Modifier.fillMaxSize())
        else -> Unit
    }
}

@Composable
private fun ResultMissedWords(misses: List<WordRushMiss>, showPerfect: Boolean) {
    if (misses.isEmpty()) {
        if (showPerfect) {
            Text(
                text = stringResource(Res.string.word_rush_perfect_round),
                style = MaterialTheme.typography.bodyLarge,
                color = AppColors.secondary,
                fontWeight = FontWeight.Medium,
            )
        }
        return
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(Theme.shapes.large),
            )
            .padding(vertical = Theme.spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.sm),
    ) {
        Text(
            text = stringResource(Res.string.word_rush_review_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = Theme.spacing.md),
        )
        // Header stays visible; only the rows scroll when they outgrow the space left.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Theme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(Theme.spacing.sm),
        ) {
            misses.forEachIndexed { index, miss ->
                if (index > 0) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }
                MissedWordRow(miss)
            }
        }
    }
}

@Composable
private fun MissedWordRow(miss: WordRushMiss) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = miss.prompt,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f, fill = false),
            )
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(horizontal = Theme.spacing.xs)
                    .size(16.dp),
            )
            Text(
                text = miss.correctAnswer,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.secondary,
                modifier = Modifier.weight(1f, fill = false),
            )
        }
        Text(
            text = miss.chosenAnswer
                ?.let { stringResource(Res.string.word_rush_review_you_picked, it) }
                ?: stringResource(Res.string.word_rush_review_timed_out),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ResultStatColumn(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color,
        )
        Spacer(Modifier.height(Theme.spacing.xxs))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}


package feature.study.ui.wordrush

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import components.LoadingScreen
import feature.study.wordrush.WordRushPhase
import feature.study.wordrush.WordRushState
import feature.study.wordrush.WordRushViewModel
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.close
import lexicon.resources.generated.resources.word_rush
import lexicon.resources.generated.resources.word_rush_best_streak_result
import lexicon.resources.generated.resources.word_rush_game_over
import lexicon.resources.generated.resources.word_rush_new_best
import lexicon.resources.generated.resources.word_rush_play_again
import lexicon.resources.generated.resources.word_rush_question_counter
import lexicon.resources.generated.resources.word_rush_result_score
import lexicon.resources.generated.resources.word_rush_score
import lexicon.resources.generated.resources.word_rush_streak
import org.jetbrains.compose.resources.stringResource
import theme.AppColors
import theme.Theme

@Composable
fun WordRushGameScreen(
    state: WordRushState,
    onSelectAnswer: (Int) -> Unit,
    onPlayAgain: () -> Unit,
    onDismiss: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(Theme.spacing.md),
    ) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.word_rush),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(Res.string.close),
                )
            }
        }

        Spacer(Modifier.height(Theme.spacing.md))

        AnimatedContent(
            targetState = state.phase,
            transitionSpec = {
                (fadeIn(tween(200)) + scaleIn(tween(200), initialScale = 0.95f))
                    .togetherWith(fadeOut(tween(150)))
            },
        ) { phase ->
            when (phase) {
                is WordRushPhase.Loading -> {
                    LoadingScreen(message = "Preparing your challenge...")
                }
                is WordRushPhase.Playing -> {
                    PlayingContent(
                        phase = phase,
                        onSelectAnswer = onSelectAnswer,
                    )
                }
                is WordRushPhase.Result -> {
                    ResultContent(
                        phase = phase,
                        onPlayAgain = onPlayAgain,
                        onDismiss = onDismiss,
                    )
                }
                is WordRushPhase.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = phase.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                is WordRushPhase.Idle -> { /* Should not appear in game screen */ }
            }
        }
    }
}

@Composable
private fun PlayingContent(
    phase: WordRushPhase.Playing,
    onSelectAnswer: (Int) -> Unit,
) {
    val timerProgress by animateFloatAsState(
        targetValue = phase.timeRemainingMs.toFloat() / WordRushViewModel.TIME_PER_QUESTION_MS,
        animationSpec = tween(durationMillis = WordRushViewModel.TIMER_TICK_MS.toInt()),
    )
    val timerColor by animateColorAsState(
        targetValue = when {
            timerProgress > 0.5f -> AppColors.secondary
            timerProgress > 0.25f -> AppColors.tertiary
            else -> AppColors.error
        },
    )

    Column(modifier = Modifier.fillMaxSize()) {
        // Score & Streak row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            StatChip(
                label = stringResource(Res.string.word_rush_score),
                value = phase.score.toString(),
                color = AppColors.primary,
            )
            StatChip(
                label = stringResource(Res.string.word_rush_question_counter, phase.questionIndex + 1, phase.totalQuestions),
                value = "",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (phase.streak > 1) {
                    Icon(
                        imageVector = Icons.Rounded.Bolt,
                        contentDescription = null,
                        tint = AppColors.tertiary,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(2.dp))
                }
                StatChip(
                    label = stringResource(Res.string.word_rush_streak),
                    value = phase.streak.toString(),
                    color = if (phase.streak > 1) AppColors.tertiary else AppColors.primary,
                )
            }
        }

        Spacer(Modifier.height(Theme.spacing.sm))

        // Timer bar
        LinearProgressIndicator(
            progress = { timerProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = timerColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )

        Spacer(Modifier.height(Theme.spacing.xl))

        // Word display
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
        ) {
            Text(
                text = phase.question.word.originalWord,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Theme.spacing.xl, horizontal = Theme.spacing.md),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }

        Spacer(Modifier.height(Theme.spacing.xl))

        // Answer options
        Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.sm)) {
            phase.question.options.forEachIndexed { index, option ->
                val isSelected = phase.selectedIndex == index
                val isCorrect = index == phase.question.correctIndex
                val hasAnswered = phase.selectedIndex != null

                val backgroundColor by animateColorAsState(
                    targetValue = when {
                        hasAnswered && isCorrect -> AppColors.secondary.copy(alpha = 0.15f)
                        hasAnswered && isSelected && !isCorrect -> AppColors.error.copy(alpha = 0.15f)
                        else -> MaterialTheme.colorScheme.surface
                    },
                )
                val borderColor by animateColorAsState(
                    targetValue = when {
                        hasAnswered && isCorrect -> AppColors.secondary
                        hasAnswered && isSelected && !isCorrect -> AppColors.error
                        else -> MaterialTheme.colorScheme.outlineVariant
                    },
                )
                val textColor by animateColorAsState(
                    targetValue = when {
                        hasAnswered && isCorrect -> AppColors.secondary
                        hasAnswered && isSelected && !isCorrect -> AppColors.error
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !hasAnswered) { onSelectAnswer(index) },
                    colors = CardDefaults.cardColors(containerColor = backgroundColor),
                    border = CardDefaults.outlinedCardBorder().let {
                        androidx.compose.foundation.BorderStroke(
                            width = if (hasAnswered && (isCorrect || isSelected)) 2.dp else 1.dp,
                            color = borderColor,
                        )
                    },
                ) {
                    Text(
                        text = option,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (hasAnswered && isCorrect) FontWeight.Bold else FontWeight.Normal,
                        color = textColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Theme.spacing.md),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun ResultContent(
    phase: WordRushPhase.Result,
    onPlayAgain: () -> Unit,
    onDismiss: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Rounded.EmojiEvents,
            contentDescription = null,
            tint = AppColors.tertiary,
            modifier = Modifier.size(72.dp),
        )

        Spacer(Modifier.height(Theme.spacing.md))

        Text(
            text = stringResource(Res.string.word_rush_game_over),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )

        Spacer(Modifier.height(Theme.spacing.sm))

        Text(
            text = stringResource(Res.string.word_rush_result_score, phase.score, phase.totalQuestions),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = AppColors.primary,
        )

        Spacer(Modifier.height(Theme.spacing.sm))

        Text(
            text = stringResource(Res.string.word_rush_best_streak_result, phase.bestStreak),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (phase.isNewBest) {
            Spacer(Modifier.height(Theme.spacing.xs))
            Text(
                text = stringResource(Res.string.word_rush_new_best),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AppColors.tertiary,
            )
        }

        Spacer(Modifier.height(Theme.spacing.xl))

        Button(
            onClick = onPlayAgain,
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.tertiary),
        ) {
            Text(
                text = stringResource(Res.string.word_rush_play_again),
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(Modifier.height(Theme.spacing.sm))

        Button(
            onClick = onDismiss,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        ) {
            Text(text = stringResource(Res.string.close))
        }
    }
}

@Composable
private fun StatChip(
    label: String,
    value: String,
    color: Color,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (value.isNotEmpty()) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color,
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = color.copy(alpha = 0.7f),
        )
    }
}

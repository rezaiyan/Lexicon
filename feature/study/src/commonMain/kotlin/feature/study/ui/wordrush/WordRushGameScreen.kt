package feature.study.ui.wordrush

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import components.ErrorScreen
import components.LoadingScreen
import feature.study.wordrush.WordRushPhase
import feature.study.wordrush.WordRushPowerUp
import feature.study.wordrush.WordRushState
import feature.study.wordrush.WordRushViewModel
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.close
import lexicon.resources.generated.resources.retry
import lexicon.resources.generated.resources.word_rush
import lexicon.resources.generated.resources.word_rush_error_body
import lexicon.resources.generated.resources.word_rush_error_title
import lexicon.resources.generated.resources.word_rush_loading
import lexicon.resources.generated.resources.word_rush_paused
import lexicon.resources.generated.resources.word_rush_paused_body
import lexicon.resources.generated.resources.word_rush_resume
import org.jetbrains.compose.resources.stringResource
import theme.Theme

@Composable
fun WordRushGameScreen(
    stateHolder: State<WordRushState>,
    onSelectAnswer: (Int) -> Unit,
    onUsePowerUp: (WordRushPowerUp) -> Unit,
    onPlayAgain: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onDismiss: () -> Unit,
) {
    // The timer must not cost lives while the app is in the background.
    // Resuming is explicit (Resume button) so the player is ready when the clock restarts.
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { onPause() }

    val isPaused by remember {
        derivedStateOf { (stateHolder.value.phase as? WordRushPhase.Playing)?.isPaused == true }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures { } },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Theme.spacing.md),
        ) {
            WordRushTopBar(onDismiss = onDismiss)
            Spacer(Modifier.height(Theme.spacing.md))
            WordRushPhaseContent(
                stateHolder = stateHolder,
                onSelectAnswer = onSelectAnswer,
                onUsePowerUp = onUsePowerUp,
                onPlayAgain = onPlayAgain,
                onDismiss = onDismiss,
            )
        }

        val floaterData by remember {
            derivedStateOf {
                (stateHolder.value.phase as? WordRushPhase.Playing)
                    ?.let { FloaterData(it.lastPointsEarned, it.isCorrect, it.earnedPowerUp) }
            }
        }
        floaterData?.let { data ->
            PointsFloater(
                lastPointsEarned = data.points,
                isCorrect = data.isCorrect,
                earnedPowerUp = data.earnedPowerUp,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        AnimatedVisibility(
            visible = isPaused,
            enter = fadeIn(tween(Theme.motion.durationShort2)),
            exit = fadeOut(tween(Theme.motion.durationXShort)),
            label = "paused-overlay",
        ) {
            PausedOverlay(onResume = onResume)
        }
    }
}

private data class FloaterData(
    val points: Int?,
    val isCorrect: Boolean?,
    val earnedPowerUp: WordRushPowerUp?,
)

@Composable
private fun PausedOverlay(onResume: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f))
            .pointerInput(Unit) { detectTapGestures { } },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .padding(Theme.spacing.xl)
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(Theme.shapes.large),
                )
                .padding(Theme.spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Theme.spacing.sm),
        ) {
            Text(
                text = stringResource(Res.string.word_rush_paused),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(Res.string.word_rush_paused_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(Theme.spacing.xs))
            Button(
                onClick = onResume,
                shape = RoundedCornerShape(Theme.shapes.pill),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(Res.string.word_rush_resume),
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun WordRushTopBar(onDismiss: () -> Unit) {
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
}

private enum class PhaseKey { Idle, Loading, Playing, Result, Error }

@Composable
private fun WordRushPhaseContent(
    stateHolder: State<WordRushState>,
    onSelectAnswer: (Int) -> Unit,
    onUsePowerUp: (WordRushPowerUp) -> Unit,
    onPlayAgain: () -> Unit,
    onDismiss: () -> Unit,
) {
    val phaseKey by remember {
        derivedStateOf {
            when (stateHolder.value.phase) {
                is WordRushPhase.Idle    -> PhaseKey.Idle
                is WordRushPhase.Loading -> PhaseKey.Loading
                is WordRushPhase.Playing -> PhaseKey.Playing
                is WordRushPhase.Result  -> PhaseKey.Result
                is WordRushPhase.Error   -> PhaseKey.Error
            }
        }
    }

    AnimatedContent(
        targetState = phaseKey,
        transitionSpec = {
            (fadeIn(tween(200)) + scaleIn(tween(200), initialScale = 0.95f))
                .togetherWith(fadeOut(tween(150)))
        },
        label = "phase-transition",
    ) { key ->
        when (key) {
            PhaseKey.Idle    -> Unit
            PhaseKey.Loading -> LoadingScreen(message = stringResource(Res.string.word_rush_loading))
            PhaseKey.Playing -> {
                val stablePhase by remember {
                    derivedStateOf {
                        (stateHolder.value.phase as? WordRushPhase.Playing)
                            ?.copy(timeRemainingMs = 0L, isTimerFrozen = false)
                    }
                }
                val sp = stablePhase ?: return@AnimatedContent
                PlayingContent(
                    phase = sp,
                    timerProgressProvider = {
                        (stateHolder.value.phase as? WordRushPhase.Playing)
                            ?.let { it.timeRemainingMs.toFloat() / WordRushViewModel.TIME_PER_QUESTION_MS }
                            ?: 0f
                    },
                    isTimerFrozenProvider = {
                        (stateHolder.value.phase as? WordRushPhase.Playing)?.isTimerFrozen ?: false
                    },
                    onSelectAnswer = onSelectAnswer,
                    onUsePowerUp = onUsePowerUp,
                )
            }
            PhaseKey.Result -> {
                val phase = stateHolder.value.phase as? WordRushPhase.Result ?: return@AnimatedContent
                ResultContent(
                    phase = phase,
                    onPlayAgain = onPlayAgain,
                    onDismiss = onDismiss,
                )
            }
            PhaseKey.Error -> ErrorScreen(
                title = stringResource(Res.string.word_rush_error_title),
                message = stringResource(Res.string.word_rush_error_body),
                icon = Icons.Rounded.ErrorOutline,
                retryLabel = stringResource(Res.string.retry),
                onRetry = onPlayAgain,
            )
        }
    }
}

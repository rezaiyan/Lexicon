package feature.study.ui.wordrush

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.Bolt
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
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import components.LoadingScreen
import feature.study.wordrush.WordRushPhase
import feature.study.wordrush.WordRushPowerUp
import feature.study.wordrush.WordRushState
import feature.study.wordrush.WordRushViewModel
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.close
import lexicon.resources.generated.resources.word_rush
import lexicon.resources.generated.resources.word_rush_accuracy
import lexicon.resources.generated.resources.word_rush_avg_speed
import lexicon.resources.generated.resources.word_rush_best_streak_result
import lexicon.resources.generated.resources.word_rush_combo
import lexicon.resources.generated.resources.word_rush_game_over
import lexicon.resources.generated.resources.word_rush_grade_a
import lexicon.resources.generated.resources.word_rush_grade_b
import lexicon.resources.generated.resources.word_rush_grade_c
import lexicon.resources.generated.resources.word_rush_grade_d
import lexicon.resources.generated.resources.word_rush_grade_s
import lexicon.resources.generated.resources.word_rush_new_best
import lexicon.resources.generated.resources.word_rush_play_again
import lexicon.resources.generated.resources.word_rush_result_score
import lexicon.resources.generated.resources.word_rush_score
import lexicon.resources.generated.resources.word_rush_streak
import org.jetbrains.compose.resources.stringResource
import theme.AppColors
import theme.Theme

private val IceBlue = Color(0xFF64B5F6)

@Composable
fun WordRushGameScreen(
    stateHolder: State<WordRushState>,
    onSelectAnswer: (Int) -> Unit,
    onUsePowerUp: (WordRushPowerUp) -> Unit,
    onPlayAgain: () -> Unit,
    onDismiss: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures { } }
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

        // derivedStateOf: phaseKey only changes on phase TYPE transitions — not on every
        // 50 ms timer tick — so WordRushGameScreen itself only recomposes on phase changes.
        val phaseKey by remember {
            derivedStateOf {
                when (stateHolder.value.phase) {
                    is WordRushPhase.Loading -> 1
                    is WordRushPhase.Playing -> 2
                    is WordRushPhase.Result  -> 3
                    is WordRushPhase.Error   -> 4
                    is WordRushPhase.Idle    -> 0
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
                1 -> LoadingScreen(message = "Preparing your challenge...")
                2 -> {
                    // stablePhase strips timeRemainingMs/isTimerFrozen so PlayingContent
                    // only recomposes on real game-state changes (answer, lives, streak),
                    // not on every 50 ms timer tick. Timer data is passed as lambdas read
                    // in the draw phase, preventing layout thrashing.
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
                3 -> {
                    val phase = stateHolder.value.phase as? WordRushPhase.Result ?: return@AnimatedContent
                    ResultContent(
                        phase = phase,
                        onPlayAgain = onPlayAgain,
                        onDismiss = onDismiss,
                    )
                }
                4 -> {
                    val phase = stateHolder.value.phase as? WordRushPhase.Error ?: return@AnimatedContent
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
            }
        }
    }
}

@Composable
private fun PlayingContent(
    phase: WordRushPhase.Playing,
    timerProgressProvider: () -> Float,
    isTimerFrozenProvider: () -> Boolean,
    onSelectAnswer: (Int) -> Unit,
    onUsePowerUp: (WordRushPowerUp) -> Unit,
) {
    val motion = Theme.motion

    // derivedStateOf: timerColorTarget re-evaluates on every 50 ms tick but the RESULT (a Color)
    // only changes when progress crosses a threshold (0.5 / 0.25) or the timer freezes.
    // PlayingContent therefore recomposes only during those 2 brief color transitions per question.
    val timerColorTarget by remember {
        derivedStateOf {
            val p = timerProgressProvider()
            when {
                isTimerFrozenProvider() -> IceBlue
                p > 0.5f -> AppColors.secondary
                p > 0.25f -> AppColors.tertiary
                else -> AppColors.error
            }
        }
    }
    val timerColor by animateColorAsState(
        targetValue = timerColorTarget,
        label = "timer-color",
    )

    val animatedScore by animateIntAsState(
        targetValue = phase.score,
        animationSpec = tween(durationMillis = motion.durationMedium),
        label = "score-counter",
    )

    // Keep State<Float> (not `by`) so the infinite transition value is only read inside
    // graphicsLayer {} (draw phase), not in composition scope — no 60 fps recompositions.
    val infiniteTransition = rememberInfiniteTransition(label = "timer-pulse")
    val timerPulseScaleState = infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "timer-pulse-scale",
    )
    // remember(phase.selectedIndex): recreate the derivedState when answer is selected so the
    // closure captures the updated selectedIndex. The derived value re-evaluates on timer ticks
    // but only notifies observers when shouldPulse actually flips (threshold crossing or answer).
    val shouldPulse by remember(phase.selectedIndex) {
        derivedStateOf {
            phase.selectedIndex == null && !isTimerFrozenProvider() && timerProgressProvider() <= 0.25f
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {

        // ── Row 1: Lives · Combo badge · Power-ups ────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LivesRow(lives = phase.lives, totalLives = WordRushViewModel.INITIAL_LIVES)

            AnimatedVisibility(
                visible = phase.multiplier > 1,
                enter = fadeIn(tween(motion.durationShort2)) + scaleIn(
                    initialScale = 0.5f,
                    animationSpec = spring(dampingRatio = 0.6f),
                ),
                exit = fadeOut(tween(motion.durationXShort)),
                label = "combo-badge",
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            color = AppColors.accentAmber.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(Theme.shapes.pill),
                        )
                        .padding(horizontal = Theme.spacing.sm, vertical = Theme.spacing.xxs),
                ) {
                    Text(
                        text = stringResource(Res.string.word_rush_combo, phase.multiplier),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.accentAmber,
                    )
                }
            }

            PowerUpsRow(
                powerUps = phase.powerUps,
                enabled = phase.selectedIndex == null,
                onUsePowerUp = onUsePowerUp,
            )
        }

        Spacer(Modifier.height(Theme.spacing.xs))

        // ── Row 2: Score · Streak ─────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = animatedScore.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.primary,
                )
                Text(
                    text = stringResource(Res.string.word_rush_score),
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.primary.copy(alpha = 0.7f),
                )
            }

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

        Spacer(Modifier.height(Theme.spacing.xs))

        // ── Progress dots ─────────────────────────────────────────────────
        ProgressDots(
            currentIndex = phase.questionIndex,
            totalCount = phase.totalQuestions,
            hasAnswered = phase.selectedIndex != null,
            isCorrect = phase.isCorrect,
        )

        Spacer(Modifier.height(Theme.spacing.sm))

        // ── Timer bar (ice-blue when frozen, pulses when low) ─────────────
        LinearProgressIndicator(
            progress = { timerProgressProvider() },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .graphicsLayer {
                    // Read .value here (draw phase) — not in composition scope — so the
                    // infinite transition drives only re-draws, not recompositions.
                    if (shouldPulse) {
                        val s = timerPulseScaleState.value
                        scaleX = s
                        scaleY = s
                    }
                },
            color = timerColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )

        Spacer(Modifier.height(Theme.spacing.xl))

        // ── Word card ─────────────────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            ),
                        ),
                    ),
            ) {
                Text(
                    text = phase.question.word.originalWord,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Theme.spacing.xxl, horizontal = Theme.spacing.md),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }

        Spacer(Modifier.height(Theme.spacing.md))

        // ── Streak fire row ───────────────────────────────────────────────
        AnimatedVisibility(
            visible = phase.streak >= 3,
            enter = fadeIn(tween(motion.durationShort2)) + slideInHorizontally { -it },
            exit = fadeOut(tween(motion.durationXShort)),
            label = "fire-row",
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                val fireCount = phase.streak.coerceAtMost(5)
                repeat(fireCount) { index ->
                    val fireScale by animateFloatAsState(
                        targetValue = 1f,
                        animationSpec = tween(
                            durationMillis = motion.durationMedium,
                            delayMillis = index * 50,
                        ),
                        label = "fire-scale-$index",
                    )
                    Text(
                        text = "\uD83D\uDD25",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.graphicsLayer {
                            scaleX = fireScale
                            scaleY = fireScale
                        },
                    )
                }
            }
        }

        Spacer(Modifier.height(Theme.spacing.md))

        // ── +Points earned indicator ──────────────────────────────────────
        AnimatedVisibility(
            visible = phase.lastPointsEarned != null && phase.isCorrect == true,
            enter = fadeIn(tween(motion.durationShort2)) + scaleIn(
                initialScale = 0.5f,
                animationSpec = spring(dampingRatio = 0.6f),
            ),
            exit = fadeOut(tween(motion.durationXLong)),
            label = "points-earned",
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "+${phase.lastPointsEarned ?: 0} pts",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.secondary,
                )
            }
        }

        Spacer(Modifier.height(Theme.spacing.sm))

        // ── Answer options ────────────────────────────────────────────────
        Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.sm)) {
            phase.question.options.forEachIndexed { index, option ->
                val isSelected = phase.selectedIndex == index
                val isCorrectOption = index == phase.question.correctIndex
                val hasAnswered = phase.selectedIndex != null
                val isHidden = phase.hiddenOptionIndices.contains(index)
                val isPeekTarget = phase.isPeeking && isCorrectOption

                val backgroundColor by animateColorAsState(
                    targetValue = when {
                        isPeekTarget -> AppColors.secondary.copy(alpha = 0.25f)
                        isHidden -> MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
                        hasAnswered && isCorrectOption -> AppColors.secondary.copy(alpha = 0.15f)
                        hasAnswered && isSelected && !isCorrectOption -> AppColors.error.copy(alpha = 0.15f)
                        else -> MaterialTheme.colorScheme.surface
                    },
                    label = "option-bg-$index",
                )
                val borderColor by animateColorAsState(
                    targetValue = when {
                        isPeekTarget -> AppColors.secondary
                        isHidden -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                        hasAnswered && isCorrectOption -> AppColors.secondary
                        hasAnswered && isSelected && !isCorrectOption -> AppColors.error
                        else -> MaterialTheme.colorScheme.outlineVariant
                    },
                    label = "option-border-$index",
                )
                val textColor by animateColorAsState(
                    targetValue = when {
                        isPeekTarget -> AppColors.secondary
                        isHidden -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                        hasAnswered && isCorrectOption -> AppColors.secondary
                        hasAnswered && isSelected && !isCorrectOption -> AppColors.error
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                    label = "option-text-$index",
                )

                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()
                val optionScale by animateFloatAsState(
                    targetValue = if (isPressed) 0.96f else 1f,
                    animationSpec = spring(stiffness = 500f),
                    label = "option-scale-$index",
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            scaleX = optionScale
                            scaleY = optionScale
                        }
                        .clickable(
                            enabled = !hasAnswered && !isHidden,
                            interactionSource = interactionSource,
                            indication = null,
                        ) { onSelectAnswer(index) },
                    colors = CardDefaults.cardColors(containerColor = backgroundColor),
                    border = androidx.compose.foundation.BorderStroke(
                        width = when {
                            isPeekTarget -> 2.dp
                            hasAnswered && (isCorrectOption || isSelected) -> 2.dp
                            else -> 1.dp
                        },
                        color = borderColor,
                    ),
                ) {
                    Text(
                        text = option,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = when {
                            isPeekTarget -> FontWeight.Bold
                            hasAnswered && isCorrectOption -> FontWeight.Bold
                            else -> FontWeight.Normal
                        },
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
private fun LivesRow(lives: Int, totalLives: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(totalLives) { index ->
            val isAlive = index < lives
            val heartScale by animateFloatAsState(
                targetValue = if (isAlive) 1f else 0.8f,
                animationSpec = spring(dampingRatio = 0.6f),
                label = "heart-scale-$index",
            )
            Text(
                text = if (isAlive) "\u2764\uFE0F" else "\uD83D\uDDA4",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.graphicsLayer {
                    scaleX = heartScale
                    scaleY = heartScale
                    alpha = if (isAlive) 1f else 0.4f
                },
            )
        }
    }
}

@Composable
private fun PowerUpsRow(
    powerUps: List<WordRushPowerUp>,
    enabled: Boolean,
    onUsePowerUp: (WordRushPowerUp) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xs)) {
        PowerUpButton(
            powerUp = WordRushPowerUp.Freeze,
            available = enabled && powerUps.contains(WordRushPowerUp.Freeze),
            onUsePowerUp = onUsePowerUp,
        )
        PowerUpButton(
            powerUp = WordRushPowerUp.FiftyFifty,
            available = enabled && powerUps.contains(WordRushPowerUp.FiftyFifty),
            onUsePowerUp = onUsePowerUp,
        )
        PowerUpButton(
            powerUp = WordRushPowerUp.Peek,
            available = enabled && powerUps.contains(WordRushPowerUp.Peek),
            onUsePowerUp = onUsePowerUp,
        )
    }
}

@Composable
private fun PowerUpButton(
    powerUp: WordRushPowerUp,
    available: Boolean,
    onUsePowerUp: (WordRushPowerUp) -> Unit,
) {
    val (emoji, color) = when (powerUp) {
        WordRushPowerUp.Freeze -> "\u2744\uFE0F" to IceBlue
        WordRushPowerUp.FiftyFifty -> "\u2702\uFE0F" to AppColors.accentAmber
        WordRushPowerUp.Peek -> "\uD83D\uDC41\uFE0F" to AppColors.tertiary
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val buttonScale by animateFloatAsState(
        targetValue = when {
            !available -> 1f
            isPressed -> 0.90f
            else -> 1f
        },
        animationSpec = spring(stiffness = 600f),
        label = "powerup-scale",
    )

    Box(
        modifier = Modifier
            .size(40.dp)
            .graphicsLayer {
                scaleX = buttonScale
                scaleY = buttonScale
                alpha = if (available) 1f else 0.25f
            }
            .background(
                color = if (available) color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(Theme.shapes.small),
            )
            .clickable(
                enabled = available,
                interactionSource = interactionSource,
                indication = null,
            ) { onUsePowerUp(powerUp) },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = emoji,
            style = MaterialTheme.typography.titleSmall,
        )
    }
}

@Composable
private fun ProgressDots(
    currentIndex: Int,
    totalCount: Int,
    hasAnswered: Boolean,
    isCorrect: Boolean?,
) {
    // Fixed height prevents layout shifts when the active dot grows/shrinks.
    // Dot scale is applied via graphicsLayer so it never affects layout.
    Row(
        modifier = Modifier.fillMaxWidth().height(10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(totalCount) { index ->
            val dotColor by animateColorAsState(
                targetValue = when {
                    index < currentIndex -> AppColors.secondary
                    index == currentIndex && hasAnswered && isCorrect == true -> AppColors.secondary
                    index == currentIndex && hasAnswered && isCorrect == false -> AppColors.error
                    index == currentIndex -> AppColors.primary
                    else -> MaterialTheme.colorScheme.outlineVariant
                },
                label = "dot-color-$index",
            )
            // Keep State<Float> so .value is read in graphicsLayer (draw phase only).
            val dotScaleState = animateFloatAsState(
                targetValue = if (index == currentIndex) 1f else 0.6f,
                animationSpec = spring(dampingRatio = 0.7f),
                label = "dot-scale-$index",
            )

            Box(
                modifier = Modifier
                    .padding(horizontal = 3.dp)
                    .size(10.dp)
                    .graphicsLayer { scaleX = dotScaleState.value; scaleY = dotScaleState.value }
                    .background(color = dotColor, shape = CircleShape),
            )
        }
    }
}

@Composable
private fun ResultContent(
    phase: WordRushPhase.Result,
    onPlayAgain: () -> Unit,
    onDismiss: () -> Unit,
) {
    val motion = Theme.motion
    val gradeColor = when (phase.grade) {
        "S" -> AppColors.accentAmber
        "A" -> AppColors.secondary
        "B" -> AppColors.primary
        "C" -> AppColors.tertiary
        "D" -> AppColors.error
        else -> AppColors.primary
    }
    val motivationalMessage = when (phase.grade) {
        "S" -> stringResource(Res.string.word_rush_grade_s)
        "A" -> stringResource(Res.string.word_rush_grade_a)
        "B" -> stringResource(Res.string.word_rush_grade_b)
        "C" -> stringResource(Res.string.word_rush_grade_c)
        else -> stringResource(Res.string.word_rush_grade_d)
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Grade badge
        Box(
            modifier = Modifier
                .size(96.dp)
                .background(color = gradeColor.copy(alpha = 0.15f), shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = phase.grade,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = gradeColor,
            )
        }

        Spacer(Modifier.height(Theme.spacing.md))

        Text(
            text = stringResource(Res.string.word_rush_game_over),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )

        Spacer(Modifier.height(Theme.spacing.xs))

        Text(
            text = motivationalMessage,
            style = MaterialTheme.typography.titleMedium,
            color = gradeColor,
            fontWeight = FontWeight.Medium,
        )

        Spacer(Modifier.height(Theme.spacing.md))

        // Lives remaining
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(WordRushViewModel.INITIAL_LIVES) { index ->
                val isAlive = index < phase.livesRemaining
                Text(
                    text = if (isAlive) "\u2764\uFE0F" else "\uD83D\uDDA4",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.graphicsLayer { alpha = if (isAlive) 1f else 0.35f },
                )
            }
        }

        Spacer(Modifier.height(Theme.spacing.lg))

        // Stats row: Score | Accuracy | Avg Speed
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            ResultStatColumn(
                value = stringResource(Res.string.word_rush_result_score, phase.score, phase.totalQuestions),
                label = stringResource(Res.string.word_rush_score),
                color = AppColors.primary,
            )
            ResultStatColumn(
                value = "${(phase.accuracy * 100).toInt()}%",
                label = stringResource(Res.string.word_rush_accuracy),
                color = AppColors.secondary,
            )
            ResultStatColumn(
                value = formatSecondsOneDecimal(phase.avgResponseTimeMs),
                label = stringResource(Res.string.word_rush_avg_speed),
                color = AppColors.tertiary,
            )
        }

        Spacer(Modifier.height(Theme.spacing.lg))

        // Best streak with fire icons
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            val fireCount = phase.bestStreak.coerceAtMost(5)
            if (fireCount > 0) {
                repeat(fireCount) {
                    Text(text = "\uD83D\uDD25", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.width(Theme.spacing.xs))
            }
            Text(
                text = stringResource(Res.string.word_rush_best_streak_result, phase.bestStreak),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

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
            shape = RoundedCornerShape(Theme.shapes.pill),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Theme.spacing.xl),
        ) {
            Text(
                text = stringResource(Res.string.word_rush_play_again),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = Theme.spacing.xs),
            )
        }

        Spacer(Modifier.height(Theme.spacing.sm))

        Button(
            onClick = onDismiss,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
            shape = RoundedCornerShape(Theme.shapes.pill),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Theme.spacing.xl),
        ) {
            Text(
                text = stringResource(Res.string.close),
                modifier = Modifier.padding(vertical = Theme.spacing.xs),
            )
        }
    }
}

/** Formats milliseconds as "X.Xs" with one decimal — KMP-safe, no String.format. */
private fun formatSecondsOneDecimal(ms: Long): String {
    val tenths = ms / 100
    return "${tenths / 10}.${tenths % 10}s"
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

@Composable
private fun StatChip(label: String, value: String, color: Color) {
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
            style = MaterialTheme.typography.labelSmall,
            color = color.copy(alpha = 0.7f),
        )
    }
}

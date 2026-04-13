package feature.study.ui.wordrush

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import components.LottieMotionIcon
import feature.study.wordrush.WordRushPhase
import feature.study.wordrush.WordRushPowerUp
import feature.study.wordrush.WordRushViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.word_rush_combo
import lexicon.resources.generated.resources.word_rush_score
import lexicon.resources.generated.resources.word_rush_streak
import org.jetbrains.compose.resources.stringResource
import theme.AppColors
import theme.Theme

private const val FIRE_LOTTIE_URL =
    "https://assets-v2.lottiefiles.com/a/9d140e5e-1121-11ef-a147-0f8f2c5fd446/12M9FMZfjS.json"

private val IceBlue = Color(0xFF64B5F6)

@Composable
internal fun PlayingContent(
    phase: WordRushPhase.Playing,
    timerProgressProvider: () -> Float,
    isTimerFrozenProvider: () -> Boolean,
    onSelectAnswer: (Int) -> Unit,
    onUsePowerUp: (WordRushPowerUp) -> Unit,
) {
    val motion = Theme.motion

    // Re-evaluates every 50ms but only notifies observers when the color threshold flips
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

    // All infinite transition State<Float>s are kept as State (not `by`) so their values
    // are only read inside graphicsLayer/drawBehind (draw phase) — no 60fps recompositions
    val infiniteTransition = rememberInfiniteTransition(label = "hud-anim")
    val timerPulseScaleState = infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "timer-pulse-scale",
    )
    val shimmerOffsetState = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer-offset",
    )
    val comboBorderAlphaState = infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "combo-glow",
    )

    val shouldPulse by remember(phase.selectedIndex) {
        derivedStateOf {
            phase.selectedIndex == null && !isTimerFrozenProvider() && timerProgressProvider() <= 0.25f
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {

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
                val isHighCombo = phase.multiplier >= 3
                Box(
                    modifier = Modifier
                        .drawBehind {
                            if (isHighCombo) {
                                drawRoundRect(
                                    color = AppColors.accentAmber.copy(alpha = comboBorderAlphaState.value),
                                    cornerRadius = CornerRadius(100f),
                                    style = Stroke(width = 2.dp.toPx()),
                                )
                            }
                        }
                        .background(
                            brush = if (isHighCombo)
                                Brush.linearGradient(
                                    listOf(
                                        AppColors.accentAmber.copy(alpha = 0.3f),
                                        AppColors.tertiary.copy(alpha = 0.3f),
                                    )
                                )
                            else
                                Brush.linearGradient(
                                    listOf(
                                        AppColors.accentAmber.copy(alpha = 0.15f),
                                        AppColors.accentAmber.copy(alpha = 0.15f),
                                    )
                                ),
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .background(
                        color = AppColors.primary.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(Theme.shapes.small),
                    )
                    .padding(horizontal = Theme.spacing.sm, vertical = Theme.spacing.xxs),
            ) {
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
                AnimatedVisibility(
                    visible = phase.streak >= 3,
                    enter = fadeIn(tween(motion.durationShort2)) + scaleIn(
                        initialScale = 0.5f,
                        animationSpec = spring(dampingRatio = 0.6f),
                    ),
                    exit = fadeOut(tween(motion.durationXShort)),
                    label = "fire-lottie",
                ) {
                    LottieMotionIcon(
                        url = FIRE_LOTTIE_URL,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Spacer(Modifier.width(2.dp))
                StatChip(
                    label = stringResource(Res.string.word_rush_streak),
                    value = phase.streak.toString(),
                    color = if (phase.streak >= 3) AppColors.tertiary else AppColors.primary,
                )
            }
        }

        Spacer(Modifier.height(Theme.spacing.xs))

        ProgressDots(
            currentIndex = phase.questionIndex,
            totalCount = phase.totalQuestions,
            hasAnswered = phase.selectedIndex != null,
            isCorrect = phase.isCorrect,
        )

        Spacer(Modifier.height(Theme.spacing.sm))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .graphicsLayer {
                    if (shouldPulse) {
                        val s = timerPulseScaleState.value
                        scaleX = s
                        scaleY = s
                    }
                },
        ) {
            LinearProgressIndicator(
                progress = { timerProgressProvider() },
                modifier = Modifier.fillMaxSize(),
                color = timerColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            // Shimmer reads shimmerOffsetState.value in draw phase only — no recomposition on timer ticks
            Box(
                modifier = Modifier.fillMaxSize().drawBehind {
                    val p = timerProgressProvider()
                    val frozen = isTimerFrozenProvider()
                    if (p <= 0.25f || frozen) {
                        val sweep = shimmerOffsetState.value
                        val tint = if (frozen) IceBlue else AppColors.error
                        drawRect(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    tint.copy(alpha = 0.55f),
                                    Color.Transparent,
                                ),
                                start = Offset(size.width * (sweep - 0.35f), 0f),
                                end = Offset(size.width * (sweep + 0.35f), size.height),
                            ),
                        )
                    }
                },
            )
        }

        Spacer(Modifier.height(Theme.spacing.lg))

        val cardBorderColor by animateColorAsState(
            targetValue = when {
                phase.selectedIndex != null && phase.isCorrect == true -> AppColors.secondary
                phase.selectedIndex != null && phase.isCorrect == false -> AppColors.error
                else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            },
            animationSpec = tween(motion.durationShort2),
            label = "card-border",
        )
        AnimatedContent(
            targetState = phase.questionIndex,
            transitionSpec = {
                if (targetState > initialState) {
                    (slideInHorizontally { it / 3 } + fadeIn(tween(250)))
                        .togetherWith(slideOutHorizontally { -it / 3 } + fadeOut(tween(200)))
                } else {
                    (slideInHorizontally { -it / 3 } + fadeIn(tween(250)))
                        .togetherWith(slideOutHorizontally { it / 3 } + fadeOut(tween(200)))
                }
            },
            label = "word-card",
        ) { _ ->
            val scheme = MaterialTheme.colorScheme
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.5.dp,
                        color = cardBorderColor,
                        shape = RoundedCornerShape(Theme.shapes.large),
                    ),
                shape = RoundedCornerShape(Theme.shapes.large),
                colors = CardDefaults.cardColors(containerColor = scheme.primaryContainer),
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "${phase.questionIndex + 1} / ${phase.totalQuestions}",
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onPrimaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(Theme.spacing.sm),
                    )
                    Text(
                        text = phase.question.word.originalWord,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = Theme.spacing.xxxl, horizontal = Theme.spacing.md),
                        color = scheme.onPrimaryContainer,
                    )
                }
            }
        }

        Spacer(Modifier.height(Theme.spacing.md))

        key(phase.questionIndex) {
            OptionsColumn(phase = phase, onSelectAnswer = onSelectAnswer)
        }
    }
}

@Composable
internal fun PointsFloater(
    lastPointsEarned: Int?,
    isCorrect: Boolean?,
    modifier: Modifier,
) {
    val offsetY = remember { Animatable(0f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(lastPointsEarned, isCorrect) {
        if (lastPointsEarned != null && isCorrect == true) {
            offsetY.snapTo(0f)
            alpha.snapTo(1f)
            launch { delay(600L); alpha.animateTo(0f, tween(300)) }
            offsetY.animateTo(-120f, tween(900, easing = FastOutLinearInEasing))
        } else {
            alpha.snapTo(0f)
        }
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                translationY = offsetY.value
                this.alpha = alpha.value
            }
            .background(
                color = AppColors.secondary.copy(alpha = 0.12f),
                shape = RoundedCornerShape(Theme.shapes.pill),
            )
            .padding(horizontal = Theme.spacing.md, vertical = Theme.spacing.xs),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "+${lastPointsEarned ?: 0} pts",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = AppColors.secondary,
        )
    }
}

@Composable
private fun OptionsColumn(
    phase: WordRushPhase.Playing,
    onSelectAnswer: (Int) -> Unit,
) {
    val letters = listOf("A", "B", "C", "D")

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

            // Staggered entrance: each option fades + scales in with 60ms delay per index
            val entranceAlpha = remember { Animatable(0f) }
            val entranceScale = remember { Animatable(0.88f) }
            LaunchedEffect(Unit) {
                delay((index * 60).toLong())
                launch { entranceAlpha.animateTo(1f, tween(180)) }
                entranceScale.animateTo(1f, spring(dampingRatio = 0.72f, stiffness = 400f))
            }

            val shakeOffsetX = remember { Animatable(0f) }
            LaunchedEffect(phase.selectedIndex) {
                if (isSelected && !isCorrectOption) {
                    repeat(3) {
                        shakeOffsetX.animateTo(8f, tween(50))
                        shakeOffsetX.animateTo(-8f, tween(50))
                    }
                    shakeOffsetX.animateTo(0f, tween(50))
                }
            }

            val popScale = remember { Animatable(1f) }
            LaunchedEffect(phase.selectedIndex) {
                if (isSelected && isCorrectOption) {
                    popScale.animateTo(1.05f, spring(dampingRatio = 0.4f, stiffness = 600f))
                    popScale.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = 400f))
                }
            }

            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            val pressScale by animateFloatAsState(
                targetValue = if (isPressed) 0.96f else 1f,
                animationSpec = spring(stiffness = 500f),
                label = "option-press-scale-$index",
            )

            val shouldHighlightBorder = isPeekTarget || (hasAnswered && (isCorrectOption || isSelected))
            val optionFontWeight = when {
                isPeekTarget || (hasAnswered && isCorrectOption) -> FontWeight.Bold
                else -> FontWeight.Normal
            }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        val combined = pressScale * popScale.value * entranceScale.value
                        scaleX = combined
                        scaleY = combined
                        translationX = shakeOffsetX.value
                        alpha = entranceAlpha.value
                    }
                    .clickable(
                        enabled = !hasAnswered && !isHidden,
                        interactionSource = interactionSource,
                        indication = null,
                    ) { onSelectAnswer(index) },
                colors = CardDefaults.cardColors(containerColor = backgroundColor),
                border = BorderStroke(
                    width = if (shouldHighlightBorder) 2.dp else 1.dp,
                    color = borderColor,
                ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Theme.spacing.sm, vertical = Theme.spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(
                                color = borderColor.copy(alpha = 0.18f),
                                shape = CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = letters.getOrElse(index) { "?" },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = borderColor,
                        )
                    }
                    Spacer(Modifier.width(Theme.spacing.sm))
                    Text(
                        text = option,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = optionFontWeight,
                        color = textColor,
                        modifier = Modifier.weight(1f),
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
            Icon(
                imageVector = if (isAlive) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = null,
                tint = if (isAlive) AppColors.error else MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer {
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
    val (icon, color) = when (powerUp) {
        WordRushPowerUp.Freeze     -> Icons.Outlined.AcUnit to IceBlue
        WordRushPowerUp.FiftyFifty -> Icons.Outlined.ContentCut to AppColors.accentAmber
        WordRushPowerUp.Peek       -> Icons.Outlined.Visibility to AppColors.tertiary
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val buttonScale by animateFloatAsState(
        targetValue = if (isPressed && available) 0.90f else 1f,
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
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (available) color else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
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
    Row(
        modifier = Modifier.fillMaxWidth(),
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
private fun StatChip(label: String, value: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(
                color = color.copy(alpha = 0.08f),
                shape = RoundedCornerShape(Theme.shapes.small),
            )
            .padding(horizontal = Theme.spacing.sm, vertical = Theme.spacing.xxs),
    ) {
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

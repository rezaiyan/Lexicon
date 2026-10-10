package feature.study.ui.listening

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import components.GradientProgressBar
import components.sheet.SheetGroup
import domain.listening.model.ListeningOrder
import domain.listening.model.ListeningSettings
import domain.listening.model.ListeningStep
import domain.listening.model.Utterance
import feature.study.listening.ListeningScreenState
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.listening_next
import lexicon.resources.generated.resources.listening_order_translation_first
import lexicon.resources.generated.resources.listening_order_word_first
import lexicon.resources.generated.resources.listening_pause_playback
import lexicon.resources.generated.resources.listening_phase_listen
import lexicon.resources.generated.resources.listening_phase_meaning
import lexicon.resources.generated.resources.listening_phase_paused
import lexicon.resources.generated.resources.listening_phase_recall
import lexicon.resources.generated.resources.listening_play
import lexicon.resources.generated.resources.listening_previous
import lexicon.resources.generated.resources.listening_round
import lexicon.resources.generated.resources.listening_seconds
import lexicon.resources.generated.resources.listening_setting_order
import lexicon.resources.generated.resources.listening_setting_pause
import lexicon.resources.generated.resources.listening_setting_repeat
import lexicon.resources.generated.resources.listening_setting_speed
import lexicon.resources.generated.resources.listening_times
import org.jetbrains.compose.resources.stringResource
import theme.AppColors
import theme.Theme
import utils.Language

private val SPEED_OPTIONS = listOf(0.75f, 1.0f, 1.25f)
private val PLAY_BUTTON_SIZE = 80.dp
private val SKIP_BUTTON_SIZE = 56.dp
private val PLAY_ICON_SIZE = 40.dp
private val SETTINGS_LABEL_WIDTH = 64.dp
private val DIVIDER_WIDTH = 40.dp
private val PROGRESS_HEIGHT = 4.dp
private val EQ_BAR_WIDTH = 3.dp
private val EQ_BAR_MIN = 4.dp
private val EQ_BAR_MAX = 16.dp
private val EQ_BAR_PERIODS_MS = listOf(420, 300, 520, 360)
private val ANSWER_RISE = 12.dp
private const val WORD_SLIDE_DIVISOR = 5
private const val WORD_ENTER_SCALE = 0.94f
private const val PLAY_ICON_ENTER_SCALE = 0.5f
private const val HALO_MAX_GROWTH = 0.22f
private const val HALO_MAX_ALPHA = 0.18f
private const val HALO_PERIOD_MS = 1_600

@Composable
internal fun PlayerContent(
    active: ListeningScreenState.Active,
    settings: ListeningSettings,
    speechRate: Float,
    showSettings: Boolean,
    actions: ListeningActions,
) {
    val session = active.session
    Column(modifier = Modifier.fillMaxSize()) {
        GradientProgressBar(
            progress = (session.index + 1f) / session.words.size,
            gradientColors = listOf(MaterialTheme.colorScheme.primary, AppColors.tertiary),
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            height = PROGRESS_HEIGHT,
            modifier = Modifier.padding(top = Theme.spacing.xs),
        )
        Spacer(Modifier.height(Theme.spacing.md))
        WordCard(
            active = active,
            settings = settings,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        )

        val motion = Theme.motion
        AnimatedVisibility(
            visible = showSettings,
            enter = expandVertically(tween(motion.durationMedium2, easing = motion.easingEmphasized)) +
                fadeIn(tween(motion.durationMedium, delayMillis = motion.durationXShort)),
            exit = shrinkVertically(tween(motion.durationMedium, easing = motion.easingStandard)) +
                fadeOut(tween(motion.durationShort)),
        ) {
            ListeningSettingsPanel(
                settings = settings,
                speechRate = speechRate,
                actions = actions,
                modifier = Modifier.padding(top = Theme.spacing.md),
            )
        }

        Spacer(Modifier.height(Theme.spacing.lg))
        TransportControls(isPlaying = active.isPlaying, actions = actions)
        Spacer(Modifier.height(Theme.spacing.md))
    }
}

// ---------------------------------------------------------------------------
// Word card
// ---------------------------------------------------------------------------

@Composable
private fun WordCard(
    active: ListeningScreenState.Active,
    settings: ListeningSettings,
    modifier: Modifier = Modifier,
) {
    val session = active.session
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(Theme.shapes.extraLarge),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = Theme.elevation.low),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Theme.gradients.primaryWash)
                .padding(Theme.spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PhaseIndicator(step = session.step, isPlaying = active.isPlaying)

            val motion = Theme.motion
            AnimatedContent(
                targetState = session.index,
                transitionSpec = {
                    val direction = if (targetState > initialState) 1 else -1
                    val enter = slideInHorizontally(tween(motion.durationLong, easing = motion.easingEmphasized)) {
                        it * direction / WORD_SLIDE_DIVISOR
                    } + fadeIn(tween(motion.durationMedium2, delayMillis = motion.durationXShort)) +
                        scaleIn(tween(motion.durationLong, easing = motion.easingEmphasized), WORD_ENTER_SCALE)
                    val exit = slideOutHorizontally(tween(motion.durationMedium, easing = motion.easingAccelerate)) {
                        -it * direction / WORD_SLIDE_DIVISOR
                    } + fadeOut(tween(motion.durationShort2)) +
                        scaleOut(tween(motion.durationMedium), WORD_ENTER_SCALE)
                    enter togetherWith exit
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                label = "listening-word",
            ) { index ->
                val shown = session.copy(index = index)
                WordFaces(
                    prompt = shown.prompt(settings.order),
                    answer = shown.answer(settings.order),
                    // Only the current word can be mid-reveal; the outgoing one fades as it was.
                    revealed = index == session.index && session.isAnswerRevealed,
                )
            }

            if (settings.repeatCount > 1) {
                Text(
                    text = stringResource(Res.string.listening_round, session.repetition + 1, settings.repeatCount),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun WordFaces(prompt: Utterance, answer: Utterance, revealed: Boolean) {
    val answerAlpha by animateFloatAsState(
        targetValue = if (revealed) 1f else 0f,
        animationSpec = tween(Theme.motion.durationMedium2, easing = Theme.motion.easingEmphasized),
        label = "listening-answer",
    )
    val riseDistance = with(LocalDensity.current) { ANSWER_RISE.toPx() }
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        LanguageLabel(prompt.languageCode)
        Spacer(Modifier.height(Theme.spacing.xs))
        Text(
            text = prompt.text,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )

        Box(
            modifier = Modifier
                .padding(vertical = Theme.spacing.lg)
                .width(DIVIDER_WIDTH)
                .height(Theme.dimensions.dividerThickness)
                .background(MaterialTheme.colorScheme.outlineVariant),
        )

        LanguageLabel(answer.languageCode)
        Spacer(Modifier.height(Theme.spacing.xs))
        // Both layers keep their space so the card never jumps when the answer appears.
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = "• • •",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.alpha((1f - answerAlpha) * Theme.opacity.disabled),
            )
            Text(
                text = answer.text,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer {
                    alpha = answerAlpha
                    translationY = (1f - answerAlpha) * riseDistance
                },
            )
        }
    }
}

@Composable
private fun LanguageLabel(languageCode: String) {
    Text(
        text = Language.fromCode(Language.toCode(languageCode)).displayName.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** What the player is doing right now, with a live equalizer while a line is being spoken. */
@Composable
private fun PhaseIndicator(step: ListeningStep, isPlaying: Boolean) {
    val speaking = isPlaying && (step == ListeningStep.Prompt || step == ListeningStep.Answer)
    val label = when {
        !isPlaying -> Res.string.listening_phase_paused
        step == ListeningStep.Prompt -> Res.string.listening_phase_listen
        step == ListeningStep.Pause -> Res.string.listening_phase_recall
        else -> Res.string.listening_phase_meaning
    }
    Row(
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.primary.copy(alpha = Theme.opacity.focus),
                shape = RoundedCornerShape(Theme.shapes.pill),
            )
            .animateContentSize()
            .padding(horizontal = Theme.spacing.sm, vertical = Theme.spacing.xxs),
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Equalizer(active = speaking)
        // Phase label rolls up as the player moves from listening to recall to meaning.
        AnimatedContent(
            targetState = label,
            transitionSpec = {
                (slideInVertically { it / 2 } + fadeIn()).togetherWith(slideOutVertically { -it / 2 } + fadeOut())
            },
            label = "listening-phase-label",
        ) { phase ->
            Text(
                text = stringResource(phase),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun Equalizer(active: Boolean) {
    val transition = rememberInfiniteTransition(label = "listening-eq")
    // Bars ease down to rest instead of snapping when speech stops.
    val activity by animateFloatAsState(
        targetValue = if (active) 1f else 0f,
        animationSpec = tween(Theme.motion.durationMedium),
        label = "listening-eq-activity",
    )
    Row(
        modifier = Modifier.height(EQ_BAR_MAX),
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xxxs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EQ_BAR_PERIODS_MS.forEach { period ->
            val fraction by transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(period), RepeatMode.Reverse),
                label = "listening-eq-bar",
            )
            Box(
                modifier = Modifier
                    .width(EQ_BAR_WIDTH)
                    .height(lerp(EQ_BAR_MIN, EQ_BAR_MAX, fraction * activity))
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(Theme.shapes.pill)),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Transport
// ---------------------------------------------------------------------------

@Composable
private fun TransportControls(isPlaying: Boolean, actions: ListeningActions) {
    // Brand tint instead of the default tonal colour, which is the green secondary.
    val skipColors = IconButtonDefaults.filledTonalIconButtonColors(
        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = Theme.opacity.focus),
        contentColor = MaterialTheme.colorScheme.primary,
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xl, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilledTonalIconButton(
            onClick = actions.onPrevious,
            modifier = Modifier.size(SKIP_BUTTON_SIZE),
            colors = skipColors,
        ) {
            Icon(Icons.Rounded.SkipPrevious, contentDescription = stringResource(Res.string.listening_previous))
        }
        PlayButton(isPlaying = isPlaying, onClick = actions.onTogglePlayback)
        FilledTonalIconButton(
            onClick = actions.onNext,
            modifier = Modifier.size(SKIP_BUTTON_SIZE),
            colors = skipColors,
        ) {
            Icon(Icons.Rounded.SkipNext, contentDescription = stringResource(Res.string.listening_next))
        }
    }
}

/** Play/pause with a soft breathing halo while playing, so the screen feels alive between words. */
@Composable
private fun PlayButton(isPlaying: Boolean, onClick: () -> Unit) {
    val haloStrength by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0f,
        animationSpec = tween(Theme.motion.durationLong),
        label = "listening-halo-strength",
    )
    Box(contentAlignment = Alignment.Center) {
        // Composed only while visible, so the pulse doesn't tick frames when paused.
        if (haloStrength > 0f) PlayHalo(strength = { haloStrength })
        FilledIconButton(onClick = onClick, modifier = Modifier.size(PLAY_BUTTON_SIZE)) {
            AnimatedContent(
                targetState = isPlaying,
                transitionSpec = {
                    (scaleIn(initialScale = PLAY_ICON_ENTER_SCALE) + fadeIn())
                        .togetherWith(scaleOut(targetScale = PLAY_ICON_ENTER_SCALE) + fadeOut())
                },
                label = "listening-play-icon",
            ) { playing ->
                Icon(
                    imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = stringResource(
                        if (playing) Res.string.listening_pause_playback else Res.string.listening_play
                    ),
                    modifier = Modifier.size(PLAY_ICON_SIZE),
                )
            }
        }
    }
}

@Composable
private fun PlayHalo(strength: () -> Float) {
    val pulse by rememberInfiniteTransition(label = "listening-halo").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(HALO_PERIOD_MS, easing = Theme.motion.easingStandard),
            RepeatMode.Reverse,
        ),
        label = "listening-halo-pulse",
    )
    val primary = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .size(PLAY_BUTTON_SIZE)
            .graphicsLayer {
                val scale = 1f + HALO_MAX_GROWTH * pulse * strength()
                scaleX = scale
                scaleY = scale
                alpha = strength() * HALO_MAX_ALPHA * (1f - pulse / 2)
            }
            .background(primary, CircleShape),
    )
}

// ---------------------------------------------------------------------------
// Settings
// ---------------------------------------------------------------------------

@Composable
internal fun ListeningSettingsPanel(
    settings: ListeningSettings,
    speechRate: Float,
    actions: ListeningActions,
    modifier: Modifier = Modifier,
) {
    SheetGroup(modifier = modifier) {
        ListeningSettingsRows(
            settings = settings,
            speechRate = speechRate,
            actions = actions,
            modifier = Modifier.padding(Theme.spacing.sm),
        )
    }
}

/** Pause, order, repeat and speed rows, without a container; shared by the player and setup. */
@Composable
internal fun ListeningSettingsRows(
    settings: ListeningSettings,
    speechRate: Float,
    actions: ListeningActions,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.xxs),
    ) {
        SettingRow(label = stringResource(Res.string.listening_setting_pause)) {
            ListeningSettings.PAUSE_OPTIONS_MS.forEach { option ->
                OptionChip(
                    text = stringResource(Res.string.listening_seconds, (option / 1_000).toInt()),
                    selected = settings.pauseMs == option,
                    onClick = { actions.onPauseSelected(option) },
                )
            }
        }
        SettingRow(label = stringResource(Res.string.listening_setting_order)) {
            ListeningOrder.entries.forEach { order ->
                OptionChip(
                    text = stringResource(
                        when (order) {
                            ListeningOrder.WORD_FIRST -> Res.string.listening_order_word_first
                            ListeningOrder.TRANSLATION_FIRST -> Res.string.listening_order_translation_first
                        }
                    ),
                    selected = settings.order == order,
                    onClick = { actions.onOrderSelected(order) },
                )
            }
        }
        SettingRow(label = stringResource(Res.string.listening_setting_repeat)) {
            for (count in ListeningSettings.MIN_REPEAT..ListeningSettings.MAX_REPEAT) {
                OptionChip(
                    text = stringResource(Res.string.listening_times, count),
                    selected = settings.repeatCount == count,
                    onClick = { actions.onRepeatSelected(count) },
                )
            }
        }
        SettingRow(label = stringResource(Res.string.listening_setting_speed)) {
            SPEED_OPTIONS.forEach { speed ->
                OptionChip(
                    text = "${speed}×",
                    selected = speechRate == speed,
                    onClick = { actions.onSpeedSelected(speed) },
                )
            }
        }
    }
}

@Composable
private fun SettingRow(label: String, chips: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(SETTINGS_LABEL_WIDTH),
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xs),
        ) {
            chips()
        }
    }
}

/** Brand-tinted chip: the default FilterChip selection colour is the green secondary. */
@Composable
internal fun OptionChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
        shape = RoundedCornerShape(Theme.shapes.pill),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = primary.copy(alpha = Theme.opacity.focus),
            selectedLabelColor = primary,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            selectedBorderColor = primary,
            borderColor = MaterialTheme.colorScheme.outlineVariant,
        ),
    )
}

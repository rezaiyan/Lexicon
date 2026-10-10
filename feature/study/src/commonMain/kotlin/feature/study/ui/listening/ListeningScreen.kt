package feature.study.ui.listening

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import components.EmptyScreen
import components.ErrorScreen
import components.LoadingScreen
import domain.listening.model.ListeningOrder
import domain.listening.model.ListeningSource
import expects.BackHandler
import feature.study.listening.ListeningScreenState
import feature.study.listening.ListeningState
import feature.study.listening.ListeningViewModel
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.close
import lexicon.resources.generated.resources.listening_done
import lexicon.resources.generated.resources.listening_empty_subtitle
import lexicon.resources.generated.resources.listening_empty_title
import lexicon.resources.generated.resources.listening_error_title
import lexicon.resources.generated.resources.listening_loading
import lexicon.resources.generated.resources.listening_no_voices_subtitle
import lexicon.resources.generated.resources.listening_no_voices_title
import lexicon.resources.generated.resources.listening_progress
import lexicon.resources.generated.resources.listening_retry
import lexicon.resources.generated.resources.listening_settings
import lexicon.resources.generated.resources.listening_title
import org.jetbrains.compose.resources.stringResource
import theme.Theme

@Composable
fun ListeningScreen(
    viewModel: ListeningViewModel,
    onDismiss: () -> Unit,
) {
    val state by viewModel.state()
    ListeningContent(
        state = state,
        actions = ListeningActions(
            onTogglePlayback = viewModel::togglePlayback,
            onNext = viewModel::next,
            onPrevious = viewModel::previous,
            onPauseSelected = viewModel::setPause,
            onOrderSelected = viewModel::setOrder,
            onRepeatSelected = viewModel::setRepeatCount,
            onSpeedSelected = viewModel::setSpeechRate,
            onDownloadVoices = viewModel::downloadMissingVoices,
            onStartWithoutVoices = viewModel::startWithoutMissingVoices,
            onSourceSelected = viewModel::selectSource,
            onLimitSelected = viewModel::setWordLimit,
            onShuffleChanged = viewModel::setShuffle,
            onStart = viewModel::start,
            onChangeWords = viewModel::open,
            onDismiss = onDismiss,
        ),
    )
}

/** Event sink lambdas for [ListeningContent]; grouped so the content stays previewable. */
@Suppress("LongParameterList") // one lambda per event-sink entry point of ListeningViewModel
class ListeningActions(
    val onTogglePlayback: () -> Unit,
    val onNext: () -> Unit,
    val onPrevious: () -> Unit,
    val onPauseSelected: (Long) -> Unit,
    val onOrderSelected: (ListeningOrder) -> Unit,
    val onRepeatSelected: (Int) -> Unit,
    val onSpeedSelected: (Float) -> Unit,
    val onDownloadVoices: () -> Unit,
    val onStartWithoutVoices: () -> Unit,
    val onSourceSelected: (ListeningSource) -> Unit,
    val onLimitSelected: (Int) -> Unit,
    val onShuffleChanged: (Boolean) -> Unit,
    /** Plays the current selection: setup's start button, "Listen again" and retry. */
    val onStart: () -> Unit,
    /** Back to setup to pick other words. */
    val onChangeWords: () -> Unit,
    val onDismiss: () -> Unit,
)

@Composable
fun ListeningContent(state: ListeningState, actions: ListeningActions) {
    var showSettings by rememberSaveable { mutableStateOf(false) }

    // System back closes the settings panel first, then leaves like the close button.
    BackHandler {
        if (showSettings) showSettings = false else actions.onDismiss()
    }

    // Opaque, tap-absorbing root: the overlay sits above the Study tab, which must not receive touches.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(Unit) { detectTapGestures { } },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Theme.spacing.md, vertical = Theme.spacing.xs),
        ) {
            ListeningTopBar(
                screen = state.screen,
                settingsOpen = showSettings,
                onToggleSettings = { showSettings = !showSettings },
                onDismiss = actions.onDismiss,
            )
            // Each phase (setup, player, summary…) eases in over the previous one instead of cutting.
            // Keyed by phase, so updates within a phase (next word, play/pause) don't re-run it.
            val motion = Theme.motion
            AnimatedContent(
                targetState = state,
                contentKey = ::phaseKey,
                transitionSpec = {
                    val enter = fadeIn(tween(motion.durationMedium2, delayMillis = motion.durationXShort)) +
                        scaleIn(
                            animationSpec = tween(motion.durationMedium2, easing = motion.easingEmphasized),
                            initialScale = PHASE_ENTER_SCALE,
                        ) +
                        slideInVertically(tween(motion.durationMedium2, easing = motion.easingEmphasized)) {
                            it / PHASE_SLIDE_DIVISOR
                        }
                    val exit = fadeOut(tween(motion.durationShort, easing = motion.easingAccelerate))
                    (enter togetherWith exit).using(SizeTransform(clip = false))
                },
                modifier = Modifier.fillMaxSize(),
                label = "listening-phase",
            ) { phaseState ->
                ListeningPhase(phaseState, showSettings, actions)
            }
        }
    }
}

private const val PHASE_ENTER_SCALE = 0.96f
private const val PHASE_SLIDE_DIVISOR = 24

/** Identity of the visible phase; [ListeningContent] animates only when this changes. */
private fun phaseKey(state: ListeningState): Any = when (val screen = state.screen) {
    ListeningScreenState.Idle, ListeningScreenState.Loading -> ListeningScreenState.Loading
    ListeningScreenState.Setup -> if (state.hasWords) ListeningScreenState.Setup else ListeningScreenState.Empty
    else -> screen::class
}

@Composable
private fun ListeningPhase(state: ListeningState, showSettings: Boolean, actions: ListeningActions) {
    when (val screen = state.screen) {
        ListeningScreenState.Idle, ListeningScreenState.Loading ->
            LoadingScreen(message = stringResource(Res.string.listening_loading))

        ListeningScreenState.Setup -> if (state.hasWords) {
            SetupContent(state, actions)
        } else {
            ListeningEmpty(actions)
        }

        ListeningScreenState.Empty -> ListeningEmpty(actions)

        ListeningScreenState.NoVoices -> EmptyScreen(
            title = stringResource(Res.string.listening_no_voices_title),
            subtitle = stringResource(Res.string.listening_no_voices_subtitle),
            icon = {
                Icon(
                    Icons.AutoMirrored.Rounded.VolumeOff,
                    contentDescription = null,
                    modifier = Modifier.size(Theme.dimensions.iconSizeHuge),
                )
            },
            actionLabel = stringResource(Res.string.listening_done),
            onAction = actions.onDismiss,
        )

        is ListeningScreenState.Error -> ErrorScreen(
            message = screen.message,
            title = stringResource(Res.string.listening_error_title),
            retryLabel = stringResource(Res.string.listening_retry),
            onRetry = actions.onStart,
        )

        is ListeningScreenState.NeedsVoices -> VoicesContent(screen, actions)
        is ListeningScreenState.Active -> PlayerContent(
            active = screen,
            settings = state.settings,
            speechRate = state.speechRate,
            showSettings = showSettings,
            actions = actions,
        )
        is ListeningScreenState.Finished -> FinishedContent(screen, actions)
    }
}

@Composable
private fun ListeningEmpty(actions: ListeningActions) {
    EmptyScreen(
        title = stringResource(Res.string.listening_empty_title),
        subtitle = stringResource(Res.string.listening_empty_subtitle),
        actionLabel = stringResource(Res.string.listening_done),
        onAction = actions.onDismiss,
    )
}

@Composable
private fun ListeningTopBar(
    screen: ListeningScreenState,
    settingsOpen: Boolean,
    onToggleSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    val active = screen as? ListeningScreenState.Active
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = stringResource(Res.string.close))
        }
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(Res.string.listening_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            AnimatedVisibility(
                visible = active != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                // Last known position keeps the label readable while it collapses away.
                val session = active?.session
                if (session != null) ProgressLabel(current = session.index + 1, total = session.words.size)
            }
        }
        // Fixed slot keeps the title centred whether or not the settings button is shown.
        Box(modifier = Modifier.size(Theme.dimensions.touchTarget), contentAlignment = Alignment.Center) {
            // Qualified: the enclosing RowScope overload would otherwise be picked and rejected.
            androidx.compose.animation.AnimatedVisibility(
                visible = active != null,
                enter = fadeIn() + scaleIn(initialScale = SETTINGS_ENTER_SCALE),
                exit = fadeOut() + scaleOut(targetScale = SETTINGS_ENTER_SCALE),
            ) {
                SettingsButton(settingsOpen = settingsOpen, onClick = onToggleSettings)
            }
        }
    }
}

private const val SETTINGS_ENTER_SCALE = 0.6f

/** "3 / 20", the number rolling up as the player advances. */
@Composable
private fun ProgressLabel(current: Int, total: Int) {
    AnimatedContent(
        targetState = current,
        transitionSpec = {
            val direction = if (targetState > initialState) 1 else -1
            (slideInVertically { it * direction } + fadeIn())
                .togetherWith(slideOutVertically { -it * direction } + fadeOut())
        },
        label = "listening-progress",
    ) { index ->
        Text(
            text = stringResource(Res.string.listening_progress, index, total),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SettingsButton(settingsOpen: Boolean, onClick: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    val containerColor by animateColorAsState(
        targetValue = if (settingsOpen) primary.copy(alpha = Theme.opacity.focus) else Color.Transparent,
        label = "listening-settings-container",
    )
    val contentColor by animateColorAsState(
        targetValue = if (settingsOpen) primary else LocalContentColor.current,
        label = "listening-settings-content",
    )
    val rotation by animateFloatAsState(
        targetValue = if (settingsOpen) SETTINGS_OPEN_ROTATION else 0f,
        label = "listening-settings-rotation",
    )
    IconButton(
        onClick = onClick,
        colors = IconButtonDefaults.iconButtonColors(containerColor = containerColor, contentColor = contentColor),
    ) {
        Icon(
            Icons.Rounded.Tune,
            contentDescription = stringResource(Res.string.listening_settings),
            modifier = Modifier.rotate(rotation),
        )
    }
}

private const val SETTINGS_OPEN_ROTATION = 90f

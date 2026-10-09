package feature.study.ui.listening

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import components.EmptyScreen
import components.ErrorScreen
import components.LoadingScreen
import domain.listening.model.ListeningOrder
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
    onRestart: () -> Unit,
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
            onRestart = onRestart,
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
    val onRestart: () -> Unit,
    val onDismiss: () -> Unit,
)

@Composable
fun ListeningContent(state: ListeningState, actions: ListeningActions) {
    var showSettings by rememberSaveable { mutableStateOf(false) }

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
            when (val screen = state.screen) {
                ListeningScreenState.Idle, ListeningScreenState.Loading ->
                    LoadingScreen(message = stringResource(Res.string.listening_loading))

                ListeningScreenState.Empty -> EmptyScreen(
                    title = stringResource(Res.string.listening_empty_title),
                    subtitle = stringResource(Res.string.listening_empty_subtitle),
                    actionLabel = stringResource(Res.string.listening_done),
                    onAction = actions.onDismiss,
                )

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
                    onRetry = actions.onRestart,
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
    }
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
            if (active != null) {
                Text(
                    text = stringResource(
                        Res.string.listening_progress,
                        active.session.index + 1,
                        active.session.words.size,
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (active != null) {
            IconButton(
                onClick = onToggleSettings,
                colors = if (settingsOpen) {
                    IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = Theme.opacity.focus),
                        contentColor = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    IconButtonDefaults.iconButtonColors()
                },
            ) {
                Icon(Icons.Rounded.Tune, contentDescription = stringResource(Res.string.listening_settings))
            }
        } else {
            // Keeps the title centred when there is no settings button.
            Spacer(Modifier.size(Theme.dimensions.touchTarget))
        }
    }
}

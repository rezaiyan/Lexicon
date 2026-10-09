package feature.study.ui.listening

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import components.EmptyScreen
import components.ErrorScreen
import components.LoadingScreen
import domain.listening.model.LanguageVoice
import domain.listening.model.ListeningOrder
import domain.listening.model.ListeningSettings
import domain.listening.model.VoiceStatus
import feature.study.listening.ListeningScreenState
import feature.study.listening.ListeningState
import feature.study.listening.ListeningViewModel
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.close
import lexicon.resources.generated.resources.listening_done
import lexicon.resources.generated.resources.listening_download_failed
import lexicon.resources.generated.resources.listening_download_voices
import lexicon.resources.generated.resources.listening_downloading
import lexicon.resources.generated.resources.listening_empty_subtitle
import lexicon.resources.generated.resources.listening_empty_title
import lexicon.resources.generated.resources.listening_error_title
import lexicon.resources.generated.resources.listening_finished_body
import lexicon.resources.generated.resources.listening_finished_title
import lexicon.resources.generated.resources.listening_listen_again
import lexicon.resources.generated.resources.listening_loading
import lexicon.resources.generated.resources.listening_next
import lexicon.resources.generated.resources.listening_no_voices_subtitle
import lexicon.resources.generated.resources.listening_no_voices_title
import lexicon.resources.generated.resources.listening_order_translation_first
import lexicon.resources.generated.resources.listening_order_word_first
import lexicon.resources.generated.resources.listening_pause_playback
import lexicon.resources.generated.resources.listening_play
import lexicon.resources.generated.resources.listening_previous
import lexicon.resources.generated.resources.listening_progress
import lexicon.resources.generated.resources.listening_recent_fallback
import lexicon.resources.generated.resources.listening_retry
import lexicon.resources.generated.resources.listening_seconds
import lexicon.resources.generated.resources.listening_setting_order
import lexicon.resources.generated.resources.listening_setting_pause
import lexicon.resources.generated.resources.listening_setting_repeat
import lexicon.resources.generated.resources.listening_setting_speed
import lexicon.resources.generated.resources.listening_start_without
import lexicon.resources.generated.resources.listening_times
import lexicon.resources.generated.resources.listening_title
import lexicon.resources.generated.resources.listening_voice_missing
import lexicon.resources.generated.resources.listening_voice_ready
import lexicon.resources.generated.resources.listening_voice_unsupported
import lexicon.resources.generated.resources.listening_voices_body
import lexicon.resources.generated.resources.listening_voices_title
import org.jetbrains.compose.resources.stringResource
import theme.Theme
import utils.Language

private val SPEED_OPTIONS = listOf(0.75f, 1.0f, 1.25f)
private const val MS_PER_MINUTE = 60_000L
private const val PERCENT = 100

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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Theme.spacing.md),
    ) {
        ListeningTopBar(screen = state.screen, onDismiss = actions.onDismiss)
        Spacer(Modifier.height(Theme.spacing.sm))
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
                icon = { Icon(Icons.Rounded.VolumeOff, contentDescription = null, modifier = Modifier.size(48.dp)) },
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
            is ListeningScreenState.Active -> PlayerContent(screen, state.settings, state.speechRate, actions)
            is ListeningScreenState.Finished -> FinishedContent(screen, actions)
        }
    }
}

@Composable
private fun ListeningTopBar(screen: ListeningScreenState, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.listening_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        if (screen is ListeningScreenState.Active) {
            Text(
                text = stringResource(
                    Res.string.listening_progress,
                    screen.session.index + 1,
                    screen.session.words.size,
                ),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = stringResource(Res.string.close))
        }
    }
}

// ---------------------------------------------------------------------------
// Player
// ---------------------------------------------------------------------------

@Composable
private fun PlayerContent(
    active: ListeningScreenState.Active,
    settings: ListeningSettings,
    speechRate: Float,
    actions: ListeningActions,
) {
    val session = active.session
    val prompt = session.prompt(settings.order)
    val answer = session.answer(settings.order)
    val answerAlpha by animateFloatAsState(if (session.isAnswerRevealed) 1f else 0f)

    Column(modifier = Modifier.fillMaxSize()) {
        LinearProgressIndicator(
            progress = { (session.index + 1f) / session.words.size },
            modifier = Modifier.fillMaxWidth(),
        )
        if (active.isRecentFallback) {
            Text(
                text = stringResource(Res.string.listening_recent_fallback),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Theme.spacing.xs),
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = prompt.text,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(Theme.spacing.md))
            Text(
                text = answer.text,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(answerAlpha),
            )
        }

        ListeningSettingsRows(settings, speechRate, actions)
        Spacer(Modifier.height(Theme.spacing.md))
        TransportControls(isPlaying = active.isPlaying, actions = actions)
    }
}

@Composable
private fun TransportControls(isPlaying: Boolean, actions: ListeningActions) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.lg, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = actions.onPrevious) {
            Icon(Icons.Rounded.SkipPrevious, contentDescription = stringResource(Res.string.listening_previous))
        }
        FilledIconButton(
            onClick = actions.onTogglePlayback,
            modifier = Modifier.size(72.dp),
            colors = IconButtonDefaults.filledIconButtonColors(),
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = stringResource(
                    if (isPlaying) Res.string.listening_pause_playback else Res.string.listening_play
                ),
                modifier = Modifier.size(40.dp),
            )
        }
        IconButton(onClick = actions.onNext) {
            Icon(Icons.Rounded.SkipNext, contentDescription = stringResource(Res.string.listening_next))
        }
    }
}

@Composable
private fun ListeningSettingsRows(settings: ListeningSettings, speechRate: Float, actions: ListeningActions) {
    Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.xs)) {
        ChipRow(label = stringResource(Res.string.listening_setting_pause)) {
            ListeningSettings.PAUSE_OPTIONS_MS.forEach { option ->
                FilterChip(
                    selected = settings.pauseMs == option,
                    onClick = { actions.onPauseSelected(option) },
                    label = { Text(stringResource(Res.string.listening_seconds, (option / 1_000).toInt())) },
                )
            }
        }
        ChipRow(label = stringResource(Res.string.listening_setting_order)) {
            ListeningOrder.entries.forEach { order ->
                FilterChip(
                    selected = settings.order == order,
                    onClick = { actions.onOrderSelected(order) },
                    label = {
                        Text(
                            stringResource(
                                when (order) {
                                    ListeningOrder.WORD_FIRST -> Res.string.listening_order_word_first
                                    ListeningOrder.TRANSLATION_FIRST -> Res.string.listening_order_translation_first
                                }
                            )
                        )
                    },
                )
            }
        }
        ChipRow(label = stringResource(Res.string.listening_setting_repeat)) {
            for (count in ListeningSettings.MIN_REPEAT..ListeningSettings.MAX_REPEAT) {
                FilterChip(
                    selected = settings.repeatCount == count,
                    onClick = { actions.onRepeatSelected(count) },
                    label = { Text(stringResource(Res.string.listening_times, count)) },
                )
            }
        }
        ChipRow(label = stringResource(Res.string.listening_setting_speed)) {
            SPEED_OPTIONS.forEach { speed ->
                FilterChip(
                    selected = speechRate == speed,
                    onClick = { actions.onSpeedSelected(speed) },
                    label = { Text("${speed}×") },
                )
            }
        }
    }
}

@Composable
private fun ChipRow(label: String, chips: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = Theme.spacing.xs),
        )
        chips()
    }
}

// ---------------------------------------------------------------------------
// Pre-flight: voices
// ---------------------------------------------------------------------------

@Composable
private fun VoicesContent(needs: ListeningScreenState.NeedsVoices, actions: ListeningActions) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.md),
    ) {
        Icon(
            Icons.Rounded.Headphones,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp),
        )
        Text(
            text = stringResource(Res.string.listening_voices_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(Res.string.listening_voices_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        needs.check.voices.forEach { VoiceRow(it) }

        Spacer(Modifier.weight(1f))

        val download = needs.download
        if (download != null) {
            Text(
                text = stringResource(
                    Res.string.listening_downloading,
                    Language.fromCode(download.languageCode).displayName,
                    download.position,
                    download.total,
                    (download.overallProgress * PERCENT).toInt(),
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            LinearProgressIndicator(progress = { download.overallProgress }, modifier = Modifier.fillMaxWidth())
        } else {
            if (needs.downloadFailed) {
                Text(
                    text = stringResource(Res.string.listening_download_failed),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Button(onClick = actions.onDownloadVoices, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.listening_download_voices))
            }
            OutlinedButton(onClick = actions.onStartWithoutVoices, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.listening_start_without))
            }
        }
    }
}

@Composable
private fun VoiceRow(voice: LanguageVoice) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = Language.fromCode(voice.languageCode).displayName,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = stringResource(
                when (voice.status) {
                    VoiceStatus.READY -> Res.string.listening_voice_ready
                    VoiceStatus.MISSING -> Res.string.listening_voice_missing
                    VoiceStatus.UNSUPPORTED -> Res.string.listening_voice_unsupported
                }
            ),
            style = MaterialTheme.typography.labelMedium,
            color = when (voice.status) {
                VoiceStatus.READY -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

// ---------------------------------------------------------------------------
// Finished
// ---------------------------------------------------------------------------

@Composable
private fun FinishedContent(finished: ListeningScreenState.Finished, actions: ListeningActions) {
    val minutes = ((finished.durationMs + MS_PER_MINUTE / 2) / MS_PER_MINUTE).toInt().coerceAtLeast(1)
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.md, Alignment.CenterVertically),
    ) {
        Icon(
            Icons.Rounded.Headphones,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp),
        )
        Text(
            text = stringResource(Res.string.listening_finished_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(Res.string.listening_finished_body, finished.wordsHeard, minutes),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Theme.spacing.md))
        Button(onClick = actions.onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(Res.string.listening_done))
        }
        OutlinedButton(onClick = actions.onRestart, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(Res.string.listening_listen_again))
        }
    }
}

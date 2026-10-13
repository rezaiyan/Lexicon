package feature.study.ui.listening

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import feature.study.listening.ListeningMiniPlayerViewModel
import feature.study.listening.MiniPlayerState
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.listening_finished_title
import lexicon.resources.generated.resources.listening_media_close
import lexicon.resources.generated.resources.listening_media_repeat
import lexicon.resources.generated.resources.listening_media_stop
import lexicon.resources.generated.resources.listening_mini_open
import lexicon.resources.generated.resources.listening_mini_words_heard
import lexicon.resources.generated.resources.listening_pause_playback
import lexicon.resources.generated.resources.listening_play
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import theme.Theme

/**
 * Compact listening player shown above the bottom bar while a session runs (or just finished)
 * with the full player collapsed. Tap opens the full player.
 */
@Composable
fun ListeningMiniPlayer(
    viewModel: ListeningMiniPlayerViewModel,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state()
    ListeningMiniPlayerContent(
        state = state,
        onOpen = onOpen,
        onTogglePlayback = viewModel::togglePlayback,
        onRepeat = viewModel::repeat,
        onClose = viewModel::close,
        modifier = modifier,
    )
}

@Composable
fun ListeningMiniPlayerContent(
    state: MiniPlayerState,
    onOpen: () -> Unit,
    onTogglePlayback: () -> Unit,
    onRepeat: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val motion = Theme.motion
    // Keep showing the last visible content while it animates out.
    val visible = state as? MiniPlayerState.Playing ?: state as? MiniPlayerState.Finished
    val lastVisible = remember { LastValue<MiniPlayerState>() }
    if (visible != null) lastVisible.value = visible
    AnimatedVisibility(
        visible = visible != null,
        enter = fadeIn(tween(motion.durationMedium)) + expandVertically(tween(motion.durationMedium)),
        exit = fadeOut(tween(motion.durationShort)) + shrinkVertically(tween(motion.durationShort)),
        modifier = modifier,
    ) {
        when (val shown = lastVisible.value) {
            is MiniPlayerState.Playing -> MiniPlayerCard(
                title = shown.title,
                subtitle = shown.subtitle,
                progress = shown.progress,
                onOpen = onOpen,
            ) {
                MiniPlayerButton(
                    icon = if (shown.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    label = stringResource(
                        if (shown.isPlaying) Res.string.listening_pause_playback else Res.string.listening_play
                    ),
                    onClick = onTogglePlayback,
                )
                MiniPlayerButton(Icons.Rounded.Close, stringResource(Res.string.listening_media_stop), onClose)
            }
            is MiniPlayerState.Finished -> MiniPlayerCard(
                title = stringResource(Res.string.listening_finished_title),
                subtitle = pluralStringResource(
                    Res.plurals.listening_mini_words_heard,
                    shown.wordsHeard,
                    shown.wordsHeard,
                ),
                progress = 1f,
                onOpen = onOpen,
            ) {
                MiniPlayerButton(Icons.Rounded.Replay, stringResource(Res.string.listening_media_repeat), onRepeat)
                MiniPlayerButton(Icons.Rounded.Close, stringResource(Res.string.listening_media_close), onClose)
            }
            else -> Unit
        }
    }
}

@Composable
private fun MiniPlayerCard(
    title: String,
    subtitle: String?,
    progress: Float,
    onOpen: () -> Unit,
    actions: @Composable () -> Unit,
) {
    val motion = Theme.motion
    val animatedProgress by animateFloatAsState(progress, tween(motion.durationMedium), label = "mini-progress")
    val shape = RoundedCornerShape(Theme.shapes.large)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Theme.spacing.screenGutter, vertical = Theme.spacing.xs),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = Theme.elevation.medium,
        shadowElevation = Theme.elevation.high,
    ) {
        Column(
            modifier = Modifier
                .clip(shape)
                .clickable(
                    onClickLabel = stringResource(Res.string.listening_mini_open),
                    role = Role.Button,
                    onClick = onOpen,
                ),
        ) {
            Row(
                modifier = Modifier.padding(
                    start = Theme.spacing.sm,
                    end = Theme.spacing.xxs,
                    top = Theme.spacing.xs,
                    bottom = Theme.spacing.xs,
                ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(Theme.dimensions.touchTargetSmall)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = Theme.opacity.focus)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Headphones,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(Theme.dimensions.iconSizeMedium),
                    )
                }
                Spacer(Modifier.width(Theme.spacing.sm))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                actions()
            }
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.fillMaxWidth().height(Theme.spacing.xxxs),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = Theme.opacity.focus),
                drawStopIndicator = {},
            )
        }
    }
}

@Composable
private fun MiniPlayerButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(Theme.dimensions.touchTarget)) {
        Icon(icon, contentDescription = label)
    }
}

/** Holds the last non-hidden state so the exit animation has something to show. */
private class LastValue<T> {
    var value: T? = null
}

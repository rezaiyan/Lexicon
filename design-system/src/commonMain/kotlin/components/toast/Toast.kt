package components.toast

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.delay
import theme.Theme

/** One toast; [id] tells repeats of the same text apart. */
@Stable
class ToastMessage internal constructor(val text: String, internal val id: Long)

/** The toast [ToastHost] shows; [show] replaces the one on screen. */
@Stable
class ToastState {
    var current: ToastMessage? by mutableStateOf(null)
        private set
    private var nextId = 0L

    fun show(text: String) {
        current = ToastMessage(text, nextId++)
    }

    /** Clears [message] if it is still the one showing. */
    fun dismiss(message: ToastMessage) {
        if (current?.id == message.id) current = null
    }
}

@Composable
fun rememberToastState(): ToastState = remember { ToastState() }

/**
 * A short, quiet notice that needs no action: shown the platform's way. Android uses its own
 * Toast, so this draws nothing there; elsewhere it is a small pill above the bottom edge.
 * Place it where a snackbar host would go (e.g. Scaffold's `snackbarHost`).
 */
@Composable
expect fun ToastHost(state: ToastState, modifier: Modifier = Modifier)

/** How long a toast stays: Android's `Toast.LENGTH_LONG`. */
internal const val TOAST_DURATION_MS = 3_500L

/** The pill for platforms without a system toast; looks like Android's. */
@Composable
internal fun ToastPill(state: ToastState, modifier: Modifier) {
    val message = state.current
    // Keeps the text while the pill fades out
    var shown by remember { mutableStateOf("") }
    LaunchedEffect(message) {
        message ?: return@LaunchedEffect
        shown = message.text
        delay(TOAST_DURATION_MS)
        state.dismiss(message)
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = Theme.spacing.screenGutter, vertical = Theme.spacing.xl),
        contentAlignment = Alignment.BottomCenter,
    ) {
        AnimatedVisibility(visible = message != null, enter = fadeIn(), exit = fadeOut()) {
            Surface(
                modifier = Modifier
                    .widthIn(max = Theme.dimensions.contentMaxWidth)
                    .semantics { liveRegion = LiveRegionMode.Polite },
                shape = RoundedCornerShape(Theme.shapes.extraLarge),
                color = MaterialTheme.colorScheme.inverseSurface,
                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
            ) {
                Text(
                    text = shown,
                    modifier = Modifier.padding(horizontal = Theme.spacing.md, vertical = Theme.spacing.sm),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

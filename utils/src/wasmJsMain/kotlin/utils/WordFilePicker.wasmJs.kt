package utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/** File picking is not supported on web yet (follow-up F-U7). */
@Composable
actual fun rememberWordFilePickerLauncher(maxBytes: Int, onPicked: (PickedFile?) -> Unit): () -> Unit =
    remember { { onPicked(null) } }

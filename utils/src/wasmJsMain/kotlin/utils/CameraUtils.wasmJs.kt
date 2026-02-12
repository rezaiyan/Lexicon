package utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * WasmJs implementation of rememberCameraLauncher
 * No-op on web platform - camera access is not supported
 */
@Composable
actual fun rememberCameraLauncher(onPhotoCaptured: (ByteArray?) -> Unit): () -> Unit {
    return remember { { onPhotoCaptured(null) } }
}

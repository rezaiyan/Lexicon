package utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * WasmJs implementation of rememberTextFilePickerLauncher
 * No-op on web platform - file picking is not supported
 */
@Composable
actual fun rememberTextFilePickerLauncher(
    onFilePicked: (content: String?, fileName: String?) -> Unit
): () -> Unit {
    return remember { { onFilePicked(null, null) } }
}

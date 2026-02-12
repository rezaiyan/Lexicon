package utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * WasmJs implementation of rememberImagePickerLauncher
 * No-op on web platform - image picking is not supported
 */
@Composable
actual fun rememberImagePickerLauncher(
    onImagePicked: (ByteArray?) -> Unit
): () -> Unit {
    return remember { { onImagePicked(null) } }
}

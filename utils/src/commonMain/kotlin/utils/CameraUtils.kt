package utils

import androidx.compose.runtime.Composable

/**
 * Remember a launcher that takes a photo using the device camera
 */
@Composable
expect fun rememberCameraLauncher(onPhotoCaptured: (ByteArray?) -> Unit): () -> Unit
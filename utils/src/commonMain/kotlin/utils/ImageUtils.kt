package utils

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Convert ByteArray to ImageBitmap in a platform-specific way
 * Handles EXIF orientation automatically
 */
expect fun ByteArray.toImageBitmap(): ImageBitmap?



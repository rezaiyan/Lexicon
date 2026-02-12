package utils

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Convert ByteArray to ImageBitmap in a platform-specific way
 * Handles EXIF orientation automatically
 */
expect fun ByteArray.toImageBitmap(): ImageBitmap?

/**
 * Image orientation from EXIF data
 */
enum class ImageOrientation(val value: Int) {
    NORMAL(1),
    FLIP_HORIZONTAL(2),
    ROTATE_180(3),
    FLIP_VERTICAL(4),
    TRANSPOSE(5),
    ROTATE_90(6),
    TRANSVERSE(7),
    ROTATE_270(8),
    UNDEFINED(0)
}


package utils

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Convert ByteArray to ImageBitmap in a platform-specific way
 * Handles EXIF orientation automatically
 */
expect fun ByteArray.toImageBitmap(): ImageBitmap?

/**
 * Compress image bytes to JPEG at the given quality (0.0 = lowest, 1.0 = highest).
 * Returns the compressed bytes, or the original if compression is not supported.
 */
expect fun ByteArray.compressImage(quality: Float): ByteArray



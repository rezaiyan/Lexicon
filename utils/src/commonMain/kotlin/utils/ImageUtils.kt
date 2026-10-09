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

/**
 * Re-encodes a photo for upload: rotated upright (EXIF), then turned [quarterTurns] × 90° clockwise,
 * longest edge at most [maxEdgePx], JPEG at [quality].
 * Returns null when the bytes are not a decodable image. Platforms without an encoder return the input unchanged.
 */
expect fun ByteArray.normalizeForUpload(maxEdgePx: Int, quality: Float, quarterTurns: Int = 0): ByteArray?



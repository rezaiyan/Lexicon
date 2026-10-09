package utils

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGContextRotateCTM
import platform.CoreGraphics.CGContextTranslateCTM
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.create
import platform.UIKit.*
import platform.posix.memcpy
import kotlin.math.PI
import org.jetbrains.skia.Image as SkiaImage

actual fun ByteArray.toImageBitmap(): ImageBitmap? {
    return try {
        val nsData = this.usePinned { pinned ->
            NSData.create(bytes = pinned.addressOf(0), length = this.size.toULong())
        }

        val uiImage = UIImage(data = nsData)
        val orientedData = UIImageJPEGRepresentation(uiImage, 0.9) ?: return null

        val orientedBytes = ByteArray(orientedData.length.toInt()).apply {
            usePinned { pinned ->
                memcpy(pinned.addressOf(0), orientedData.bytes, orientedData.length)
            }
        }

        SkiaImage.makeFromEncoded(orientedBytes).toComposeImageBitmap()
    } catch (_: Exception) {
        try {
            SkiaImage.makeFromEncoded(this).toComposeImageBitmap()
        } catch (_: Exception) {
            null
        }
    }
}

actual fun ByteArray.compressImage(quality: Float): ByteArray {
    return try {
        val nsData = this.usePinned { pinned ->
            NSData.create(bytes = pinned.addressOf(0), length = this.size.toULong())
        }
        val uiImage = UIImage(data = nsData)
        val compressedData = UIImageJPEGRepresentation(uiImage, quality.toDouble().coerceIn(0.0, 1.0)) ?: return this
        ByteArray(compressedData.length.toInt()).apply {
            usePinned { pinned ->
                memcpy(pinned.addressOf(0), compressedData.bytes, compressedData.length)
            }
        }
    } catch (_: Exception) {
        this
    }
}

@OptIn(ExperimentalForeignApi::class)
actual fun ByteArray.normalizeForUpload(maxEdgePx: Int, quality: Float, quarterTurns: Int): ByteArray? {
    val nsData = usePinned { pinned -> NSData.create(bytes = pinned.addressOf(0), length = size.toULong()) }
    val image = UIImage.imageWithData(nsData) ?: return null
    val (width, height) = image.size.useContents { width to height }
    if (width <= 0.0 || height <= 0.0) return null
    val scale = minOf(1.0, maxEdgePx / maxOf(width, height))
    val upright = image.redrawn(width * scale, height * scale, quarterTurns.mod(4)) ?: return null
    return UIImageJPEGRepresentation(upright, quality.toDouble().coerceIn(0.0, 1.0))?.toByteArray()
}

/**
 * Drawing applies imageOrientation, so the result is upright with no EXIF dependency; it is then
 * turned [quarterTurns] × 90° clockwise (UIKit's flipped y axis makes a positive angle clockwise).
 */
@OptIn(ExperimentalForeignApi::class)
private fun UIImage.redrawn(width: Double, height: Double, quarterTurns: Int): UIImage? {
    val sideways = quarterTurns % 2 == 1
    val outWidth = if (sideways) height else width
    val outHeight = if (sideways) width else height
    UIGraphicsBeginImageContextWithOptions(CGSizeMake(outWidth, outHeight), true, 1.0)
    UIGraphicsGetCurrentContext()?.takeIf { quarterTurns != 0 }?.let { context ->
        CGContextTranslateCTM(context, outWidth / 2, outHeight / 2)
        CGContextRotateCTM(context, quarterTurns * PI / 2)
        CGContextTranslateCTM(context, -width / 2, -height / 2)
    }
    drawInRect(CGRectMake(0.0, 0.0, width, height))
    val drawn = UIGraphicsGetImageFromCurrentImageContext()
    UIGraphicsEndImageContext()
    return drawn
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray = ByteArray(length.toInt()).apply {
    if (isNotEmpty()) usePinned { pinned -> memcpy(pinned.addressOf(0), bytes, length) }
}

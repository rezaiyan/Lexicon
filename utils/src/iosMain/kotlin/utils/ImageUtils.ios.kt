package utils

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.create
import platform.UIKit.*
import platform.posix.memcpy
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




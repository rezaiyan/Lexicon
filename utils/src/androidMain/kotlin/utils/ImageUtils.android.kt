package utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

actual fun ByteArray.toImageBitmap(): ImageBitmap? {
    return try {
        val bitmap = BitmapFactory.decodeByteArray(this, 0, this.size) ?: return null
        bitmap.applyExifOrientation(this).asImageBitmap()
    } catch (e: Exception) {
        try {
            val bitmap = BitmapFactory.decodeByteArray(this, 0, this.size)
            bitmap?.asImageBitmap()
        } catch (e2: Exception) {
            null
        }
    }
}

private fun Bitmap.applyExifOrientation(source: ByteArray): Bitmap {
    val orientation = ExifInterface(ByteArrayInputStream(source)).getAttributeInt(
        ExifInterface.TAG_ORIENTATION,
        ExifInterface.ORIENTATION_NORMAL
    )
    return when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> rotateBitmap(this, 90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> rotateBitmap(this, 180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> rotateBitmap(this, 270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> flipBitmap(this, horizontal = true)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> flipBitmap(this, vertical = true)
        ExifInterface.ORIENTATION_TRANSPOSE -> transposeBitmap(this)
        ExifInterface.ORIENTATION_TRANSVERSE -> transverseBitmap(this)
        else -> this
    }
}

@Suppress("SwallowedException")
actual fun ByteArray.normalizeForUpload(maxEdgePx: Int, quality: Float): ByteArray? = try {
    decodeSampled(maxEdgePx)
        ?.applyExifOrientation(this)
        ?.scaledToFit(maxEdgePx)
        ?.toJpeg(quality)
} catch (_: IllegalArgumentException) {
    null
} catch (_: OutOfMemoryError) {
    null
}

/** Decodes at the smallest power-of-two sample that still covers [maxEdgePx], so big photos fit in memory. */
private fun ByteArray.decodeSampled(maxEdgePx: Int): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(this, 0, size, bounds)
    val longest = maxOf(bounds.outWidth, bounds.outHeight)
    if (longest <= 0) return null
    var sample = 1
    while (longest / (sample * 2) >= maxEdgePx) sample *= 2
    return BitmapFactory.decodeByteArray(this, 0, size, BitmapFactory.Options().apply { inSampleSize = sample })
}

private fun Bitmap.scaledToFit(maxEdgePx: Int): Bitmap {
    val scale = maxEdgePx / maxOf(width, height).toFloat()
    if (scale >= 1f) return this
    return Bitmap.createScaledBitmap(this, (width * scale).toInt(), (height * scale).toInt(), true)
}

private fun Bitmap.toJpeg(quality: Float): ByteArray = ByteArrayOutputStream().also { out ->
    compress(Bitmap.CompressFormat.JPEG, (quality.coerceIn(0f, 1f) * 100).toInt(), out)
}.toByteArray()

private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
    val matrix = Matrix().apply { postRotate(degrees) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

private fun flipBitmap(bitmap: Bitmap, horizontal: Boolean = false, vertical: Boolean = false): Bitmap {
    val matrix = Matrix().apply {
        if (horizontal) postScale(-1f, 1f, bitmap.width / 2f, bitmap.height / 2f)
        if (vertical) postScale(1f, -1f, bitmap.width / 2f, bitmap.height / 2f)
    }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

private fun transposeBitmap(bitmap: Bitmap): Bitmap {
    val matrix = Matrix().apply {
        postRotate(90f)
        postScale(-1f, 1f, bitmap.height / 2f, bitmap.width / 2f)
    }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

private fun transverseBitmap(bitmap: Bitmap): Bitmap {
    val matrix = Matrix().apply {
        postRotate(270f)
        postScale(-1f, 1f, bitmap.height / 2f, bitmap.width / 2f)
    }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

actual fun ByteArray.compressImage(quality: Float): ByteArray {
    return try {
        val bitmap = BitmapFactory.decodeByteArray(this, 0, this.size) ?: return this
        val qualityInt = (quality.coerceIn(0f, 1f) * 100).toInt()
        val output = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, qualityInt, output)
        output.toByteArray()
    } catch (_: Exception) {
        this
    }
}

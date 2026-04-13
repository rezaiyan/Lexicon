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
        var bitmap = BitmapFactory.decodeByteArray(this, 0, this.size) ?: return null
        
        val exif = ExifInterface(ByteArrayInputStream(this))
        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
        
        bitmap = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotateBitmap(bitmap, 90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> rotateBitmap(bitmap, 180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> rotateBitmap(bitmap, 270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> flipBitmap(bitmap, horizontal = true)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> flipBitmap(bitmap, vertical = true)
            ExifInterface.ORIENTATION_TRANSPOSE -> transposeBitmap(bitmap)
            ExifInterface.ORIENTATION_TRANSVERSE -> transverseBitmap(bitmap)
            else -> bitmap
        }
        
        bitmap.asImageBitmap()
    } catch (e: Exception) {
        try {
            val bitmap = BitmapFactory.decodeByteArray(this, 0, this.size)
            bitmap?.asImageBitmap()
        } catch (e2: Exception) {
            null
        }
    }
}

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

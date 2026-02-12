package utils

import androidx.compose.ui.graphics.ImageBitmap

/**
 * WasmJs implementation of ByteArray.toImageBitmap
 * Returns null as image conversion is not supported on web
 */
actual fun ByteArray.toImageBitmap(): ImageBitmap? = null

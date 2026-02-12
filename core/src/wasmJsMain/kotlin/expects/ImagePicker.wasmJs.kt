package expects

/**
 * WasmJs implementation of ImagePicker
 * No-op on web platform
 */
actual class ImagePicker actual constructor() {
    actual suspend fun pickImage(): ByteArray? = null
    actual suspend fun takePhoto(): ByteArray? = null
}

package expects

expect class ImagePicker() {
    suspend fun pickImage(): ByteArray?
    suspend fun takePhoto(): ByteArray?
}
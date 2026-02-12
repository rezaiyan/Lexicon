package utils

import androidx.compose.runtime.Composable

@Composable
expect fun rememberImagePickerLauncher(
    onImagePicked: (ByteArray?) -> Unit
): () -> Unit


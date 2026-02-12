package utils

import androidx.compose.runtime.*
import expects.ImagePicker
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
actual fun rememberCameraLauncher(onPhotoCaptured: (ByteArray?) -> Unit): () -> Unit {
    val scope = rememberCoroutineScope()
    val imagePicker = remember { ImagePicker() }
    
    return remember {
        {
            scope.launch {
                try {
                    val imageBytes = imagePicker.takePhoto()
                    withContext(Dispatchers.Main) {
                        onPhotoCaptured(imageBytes)
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        onPhotoCaptured(null)
                    }
                }
            }
        }
    }
}




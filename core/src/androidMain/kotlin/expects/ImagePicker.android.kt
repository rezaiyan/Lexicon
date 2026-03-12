package expects

import android.content.Context
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume

actual class ImagePicker {
    private var currentContext: Context? = null
    private var photoUri: Uri? = null
    
    fun setContext(context: Context) {
        currentContext = context
    }
    
    actual suspend fun pickImage(): ByteArray? {
        return null
    }
    
    actual suspend fun takePhoto(): ByteArray? = suspendCancellableCoroutine { continuation ->
        val context = currentContext ?: run {
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }
        
        val activity = context as? ComponentActivity ?: run {
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }
        
        try {
            val photoFile = File.createTempFile(
                "IMG_",
                ".jpg",
                context.cacheDir
            ).apply {
                createNewFile()
                deleteOnExit()
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                photoFile
            )
            photoUri = uri

            val launcher = activity.activityResultRegistry.register(
                "camera_capture_${System.currentTimeMillis()}",
                ActivityResultContracts.TakePicture()
            ) { success: Boolean ->
                val capturedUri = photoUri
                if (success && capturedUri != null) {
                    try {
                        val bytes = context.contentResolver.openInputStream(capturedUri)?.use { it.readBytes() }
                        continuation.resume(bytes)
                    } catch (e: Exception) {
                        continuation.resume(null)
                    }
                } else {
                    continuation.resume(null)
                }
            }

            launcher.launch(uri)
            
            continuation.invokeOnCancellation {
                photoFile.delete()
            }
        } catch (e: Exception) {
            continuation.resume(null)
        }
    }
}


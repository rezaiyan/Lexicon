package utils

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File

@Composable
actual fun rememberCameraLauncher(onPhotoCaptured: (ByteArray?) -> Unit): () -> Unit {
    val context = LocalContext.current
    var capturedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var shouldLaunchCamera by remember { mutableStateOf(false) }
    
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = capturedPhotoUri
        if (success && uri != null) {
            try {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                onPhotoCaptured(bytes)
            } catch (e: Exception) {
                onPhotoCaptured(null)
            }
        } else {
            onPhotoCaptured(null)
        }
        capturedPhotoUri = null
    }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted && shouldLaunchCamera) {
            shouldLaunchCamera = false
            launchCamera(context) { uri ->
                capturedPhotoUri = uri
            }?.let { uri ->
                cameraLauncher.launch(uri)
            }
        } else {
            onPhotoCaptured(null)
            shouldLaunchCamera = false
        }
    }
    
    return {
        when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) -> {
                launchCamera(context) { uri ->
                    capturedPhotoUri = uri
                }?.let { uri ->
                    cameraLauncher.launch(uri)
                }
            }
            else -> {
                shouldLaunchCamera = true
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }
}

private fun launchCamera(context: android.content.Context, onUriCreated: (Uri) -> Unit): Uri? {
    return try {
        val photoFile = File.createTempFile(
            "CAMERA_",
            ".jpg",
            context.cacheDir
        )
        
        val uri = FileProvider.getUriForFile(
            context,
            "com.alirezaiyan.vokab.fileprovider",
            photoFile
        )
        
        onUriCreated(uri)
        uri
    } catch (e: Exception) {
        null
    }
}




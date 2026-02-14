@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package presentation.ui.permissions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

// Top-level helper functions for js() calls (required for Kotlin/Wasm)
private fun isNotificationSupported(): Boolean =
    js("typeof window !== 'undefined' && typeof window.Notification !== 'undefined'")

private fun getNotificationPermission(): String =
    js("window.Notification ? window.Notification.permission : 'denied'")

private fun requestNotificationPermissionJs(onResult: (String) -> Unit): Unit =
    js("window.Notification.requestPermission().then(result => onResult(result))")

private suspend fun requestNotificationPermissionAsync(): String =
    suspendCancellableCoroutine { continuation ->
        requestNotificationPermissionJs { result ->
            continuation.resume(result)
        }
    }

@Composable
actual fun rememberNotificationPermissionRequester(
    onResult: (Boolean) -> Unit
): () -> Unit {
    val scope = rememberCoroutineScope()
    return remember {
        {
            if (!isNotificationSupported()) {
                onResult(false)
            } else {
                val permission = getNotificationPermission()
                when (permission) {
                    "granted" -> {
                        onResult(true)
                    }
                    "denied" -> {
                        onResult(false)
                    }
                    else -> {
                        // "default" - request permission
                        scope.launch {
                            try {
                                val result = requestNotificationPermissionAsync()
                                val granted = result == "granted"
                                onResult(granted)
                            } catch (e: Exception) {
                                onResult(false)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
actual fun wasNotificationPermissionDenied(): Boolean {
    if (!isNotificationSupported()) return false
    return getNotificationPermission() == "denied"
}

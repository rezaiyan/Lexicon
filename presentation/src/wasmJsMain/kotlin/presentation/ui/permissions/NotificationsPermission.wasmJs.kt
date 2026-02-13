package presentation.ui.permissions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.browser.window

@Composable
actual fun rememberNotificationPermissionRequester(
    onResult: (Boolean) -> Unit
): () -> Unit {
    return remember {
        {
            val notification = js("window.Notification")
            if (notification != undefined) {
                val permission = js("Notification.permission").toString()
                when (permission) {
                    "granted" -> {
                        onResult(true)
                    }
                    "denied" -> {
                        onResult(false)
                    }
                    else -> {
                        // "default" - request permission
                        js("Notification.requestPermission()")
                            .then { result: dynamic ->
                                val granted = result.toString() == "granted"
                                onResult(granted)
                            }
                            .catch {
                                onResult(false)
                            }
                    }
                }
            } else {
                // Notifications not supported
                onResult(false)
            }
        }
    }
}

@Composable
actual fun wasNotificationPermissionDenied(): Boolean {
    val notification = js("window.Notification")
    if (notification == undefined) return false

    val permission = js("Notification.permission").toString()
    return permission == "denied"
}

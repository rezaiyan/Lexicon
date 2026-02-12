package presentation.ui.permissions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import domain.notifications.repository.INotificationRepository
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
actual fun rememberNotificationPermissionRequester(
    onResult: (Boolean) -> Unit
): () -> Unit {
    val notificationRepository = koinInject<INotificationRepository>()
    val coroutineScope = rememberCoroutineScope()
    
    return remember {
        {
            coroutineScope.launch {
                val granted = notificationRepository.requestNotificationPermission()
                onResult(granted)
            }
        }
    }
}

@Composable
actual fun wasNotificationPermissionDenied(): Boolean {
    val notificationRepository = koinInject<INotificationRepository>()
    var denied by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        denied = notificationRepository.wasNotificationPermissionDenied()
    }
    
    return denied
}



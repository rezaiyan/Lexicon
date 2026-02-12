package notification

import domain.auth.repository.IAuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationHandler(
    private val filter: NotificationFilter,
    private val authRepository: IAuthRepository
) {
    suspend fun shouldProcessNotification(
        category: NotificationCategory
    ): Boolean {
        return filter.shouldShowNotification(category, authRepository)
    }
    
    fun processNotificationAsync(
        category: NotificationCategory,
        onShouldShow: suspend () -> Unit,
        onShouldSkip: (() -> Unit)? = null
    ) {
        CoroutineScope(Dispatchers.Default).launch {
            val shouldShow = shouldProcessNotification(category)
            if (shouldShow) {
                onShouldShow()
            } else {
                onShouldSkip?.invoke()
            }
        }
    }
}


package feature.settings

import core.common.getOrDefault
import domain.notifications.repository.INotificationRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart

class NotificationPermissionMonitor(
    private val notificationRepository: INotificationRepository
) {
    private val refreshTrigger = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1)

    @OptIn(ExperimentalCoroutinesApi::class)
    val systemNotificationsEnabled: Flow<Boolean> = refreshTrigger
        .onStart { emit(Unit) }
        .flatMapLatest {
            flow {
                emit(notificationRepository.areNotificationsEnabled().getOrDefault(false))
            }
        }
        .catch { emit(false) }

    suspend fun refresh() {
        refreshTrigger.emit(Unit)
    }
}

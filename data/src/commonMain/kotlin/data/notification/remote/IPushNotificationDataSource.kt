package data.notification.remote

import data.notification.remote.model.RegisterPushTokenRequest
import core.common.Try

interface IPushNotificationDataSource {
    suspend fun registerPushToken(request: RegisterPushTokenRequest): Try<Unit>
    suspend fun deactivateAllTokens(): Try<Unit>
}

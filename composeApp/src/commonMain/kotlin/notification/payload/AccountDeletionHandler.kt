package notification.payload

import domain.auth.usecase.ClearAllUserDataUseCase

class AccountDeletionHandler(
    private val clearAllUserDataUseCase: ClearAllUserDataUseCase
) : NotificationPayloadHandler {

    override val type: String = "account_deleted"

    override suspend fun handle(data: Map<String, String>) {
        clearAllUserDataUseCase()
    }
}


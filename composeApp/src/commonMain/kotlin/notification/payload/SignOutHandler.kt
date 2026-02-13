package notification.payload

import domain.auth.usecase.ClearAllUserDataUseCase

class SignOutHandler(
    private val clearAllUserDataUseCase: ClearAllUserDataUseCase
) : NotificationPayloadHandler {

    override val type: String = "sign_out"

    override suspend fun handle(data: Map<String, String>) {
        clearAllUserDataUseCase()
    }
}


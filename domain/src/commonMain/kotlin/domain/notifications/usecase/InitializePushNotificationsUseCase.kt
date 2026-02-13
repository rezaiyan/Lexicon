package domain.notifications.usecase

import domain.auth.usecase.IsAuthenticatedUseCase

/**
 * Use case to initialize push notifications after successful authentication.
 * Only registers push token if user is authenticated.
 */
class InitializePushNotificationsUseCase(
    private val isAuthenticatedUseCase: IsAuthenticatedUseCase,
    private val registerPushTokenUseCase: RegisterPushTokenUseCase
) {
    suspend operator fun invoke() {
        if (isAuthenticatedUseCase()) {
            registerPushTokenUseCase.initializeAndRegister()
        }
    }
}

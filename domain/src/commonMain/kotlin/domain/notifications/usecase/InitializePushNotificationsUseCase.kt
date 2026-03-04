package domain.notifications.usecase

import core.common.NoParamUseCase
import core.common.Try
import core.common.getOrThrow
import domain.auth.usecase.IsAuthenticatedUseCase

/**
 * Use case to initialize push notifications after successful authentication.
 * Only registers push token if user is authenticated.
 */
class InitializePushNotificationsUseCase(
    private val isAuthenticatedUseCase: IsAuthenticatedUseCase,
    private val registerPushTokenUseCase: RegisterPushTokenUseCase
) : NoParamUseCase<Unit> {
    override suspend operator fun invoke(params: Unit) = invoke()

    suspend operator fun invoke(): Try<Unit> = Try {
        if (isAuthenticatedUseCase().getOrThrow()) {
            registerPushTokenUseCase.initializeAndRegister()
        }
    }
}

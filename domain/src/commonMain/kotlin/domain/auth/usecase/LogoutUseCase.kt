package domain.auth.usecase

import domain.auth.service.IAuthenticationService
import domain.common.Try
import kotlinx.coroutines.flow.Flow

class LogoutUseCase(
    private val authenticationService: IAuthenticationService
) {
    fun invoke(): Flow<Try<Unit>> = authenticationService.logout()
}

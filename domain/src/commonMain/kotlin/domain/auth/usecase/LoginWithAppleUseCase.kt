package domain.auth.usecase

import domain.auth.model.AuthUser
import domain.auth.service.IAuthenticationService
import core.common.Try
import kotlinx.coroutines.flow.Flow

class LoginWithAppleUseCase(
    private val authenticationService: IAuthenticationService
) {
    fun invoke(idToken: String, fullName: String?, appleUserId: String): Flow<Try<AuthUser>> =
        authenticationService.loginWithApple(idToken, fullName, appleUserId)
}

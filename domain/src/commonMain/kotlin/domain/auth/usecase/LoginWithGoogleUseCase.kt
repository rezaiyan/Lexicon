package domain.auth.usecase

import domain.auth.model.AuthUser
import domain.auth.service.IAuthenticationService
import core.common.Try
import kotlinx.coroutines.flow.Flow

class LoginWithGoogleUseCase(
    private val authenticationService: IAuthenticationService
) {
    fun invoke(idToken: String): Flow<Try<AuthUser>> = authenticationService.loginWithGoogle(idToken)
}

package domain.auth.usecase

import core.common.FlowUseCase
import domain.auth.model.AuthUser
import domain.auth.service.IAuthenticationService
import kotlinx.coroutines.flow.Flow

class LoginWithGoogleUseCase(
    private val authenticationService: IAuthenticationService
) : FlowUseCase<String, AuthUser> {

    override operator fun invoke(params: String): Flow<AuthUser> =
        authenticationService.loginWithGoogle(params)
}

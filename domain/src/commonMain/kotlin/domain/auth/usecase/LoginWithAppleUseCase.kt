package domain.auth.usecase

import core.common.FlowUseCase
import domain.auth.model.AuthUser
import domain.auth.service.IAuthenticationService
import kotlinx.coroutines.flow.Flow

class LoginWithAppleUseCase(
    private val authenticationService: IAuthenticationService
) : FlowUseCase<LoginWithAppleUseCase.Params, AuthUser> {

    data class Params(val idToken: String, val fullName: String?, val appleUserId: String)

    override operator fun invoke(params: Params): Flow<AuthUser> =
        invoke(params.idToken, params.fullName, params.appleUserId)

    fun invoke(idToken: String, fullName: String?, appleUserId: String): Flow<AuthUser> =
        authenticationService.loginWithApple(idToken, fullName, appleUserId)
}

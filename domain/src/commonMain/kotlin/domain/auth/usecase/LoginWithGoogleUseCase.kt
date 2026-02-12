package domain.auth.usecase

import domain.auth.model.AuthUser
import domain.auth.service.IAuthenticationService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LoginWithGoogleUseCase(
    private val authenticationService: IAuthenticationService
) {
    fun invoke(idToken: String): Flow<AuthResult> = authenticationService.loginWithGoogle(idToken)
        .map { serviceResult ->
            when (serviceResult) {
                is IAuthenticationService.AuthResult.Success -> AuthResult.Success(serviceResult.user)
                is IAuthenticationService.AuthResult.Error -> AuthResult.Error(serviceResult.message)
            }
        }
    
    sealed class AuthResult {
        data class Success(val user: AuthUser) : AuthResult()
        data class Error(val message: String) : AuthResult()
    }
}
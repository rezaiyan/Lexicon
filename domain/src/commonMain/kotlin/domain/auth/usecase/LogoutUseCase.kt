package domain.auth.usecase

import domain.auth.service.IAuthenticationService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LogoutUseCase(
    private val authenticationService: IAuthenticationService
) {
    fun invoke(): Flow<LogoutResult> = authenticationService.logout()
        .map { serviceResult ->
            when (serviceResult) {
                is IAuthenticationService.AuthResult.Success -> LogoutResult.Success
                is IAuthenticationService.AuthResult.Error -> LogoutResult.Error(serviceResult.message)
            }
        }
    
    sealed class LogoutResult {
        data object Success : LogoutResult()
        data class Error(val message: String) : LogoutResult()
    }
}
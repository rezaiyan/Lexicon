package domain.auth.usecase

import domain.auth.service.IAuthenticationService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DeleteAccountUseCase(
    private val authenticationService: IAuthenticationService
) {
    fun invoke(): Flow<DeleteAccountResult> = authenticationService.deleteAccount()
        .map { serviceResult ->
            when (serviceResult) {
                is IAuthenticationService.AuthResult.Success -> DeleteAccountResult.Success
                is IAuthenticationService.AuthResult.Error -> DeleteAccountResult.Error(serviceResult.message)
            }
        }
    
    sealed class DeleteAccountResult {
        data object Success : DeleteAccountResult()
        data class Error(val message: String) : DeleteAccountResult()
    }
}
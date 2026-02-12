package domain.auth.service

import domain.auth.model.AuthUser
import domain.auth.repository.IAuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

interface IAuthenticationService {
    fun loginWithGoogle(idToken: String): Flow<AuthResult>
    fun loginWithApple(idToken: String, fullName: String?, appleUserId: String): Flow<AuthResult>
    fun logout(): Flow<AuthResult>
    fun deleteAccount(): Flow<AuthResult>
    
    sealed interface AuthResult {
        data class Success(val user: AuthUser) : AuthResult
        data class Error(val message: String) : AuthResult
    }
}

class AuthenticationService(
    private val authRepository: IAuthRepository
) : IAuthenticationService {
    
    override fun loginWithGoogle(idToken: String): Flow<IAuthenticationService.AuthResult> = flow {
        val result = authRepository.loginWithGoogle(idToken)
        result.fold(
            onSuccess = { user ->
                emit(IAuthenticationService.AuthResult.Success(user))
            },
            onFailure = { error ->
                emit(IAuthenticationService.AuthResult.Error(error.message ?: "Login failed"))
            }
        )
    }
    
    override fun loginWithApple(idToken: String, fullName: String?, appleUserId: String): Flow<IAuthenticationService.AuthResult> = flow {
        val result = authRepository.loginWithApple(idToken, fullName, appleUserId)
        result.fold(
            onSuccess = { user ->
                emit(IAuthenticationService.AuthResult.Success(user))
            },
            onFailure = { error ->
                emit(IAuthenticationService.AuthResult.Error(error.message ?: "Login failed"))
            }
        )
    }
    
    override fun logout(): Flow<IAuthenticationService.AuthResult> = flow {
        val result = authRepository.logout()
        result.fold(
            onSuccess = {
                emit(IAuthenticationService.AuthResult.Success(AuthUser(id = 0L, email = "", name = "", profileImageUrl = null)))
            },
            onFailure = { error ->
                emit(IAuthenticationService.AuthResult.Error(error.message ?: "Logout failed"))
            }
        )
    }
    
    override fun deleteAccount(): Flow<IAuthenticationService.AuthResult> = flow {
        val result = authRepository.deleteAccount()
        result.fold(
            onSuccess = {
                emit(IAuthenticationService.AuthResult.Success(AuthUser(id = 0L, email = "", name = "", profileImageUrl = null)))
            },
            onFailure = { error ->
                emit(IAuthenticationService.AuthResult.Error(error.message ?: "Delete account failed"))
            }
        )
    }
}

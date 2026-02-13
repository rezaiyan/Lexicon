package domain.auth.service

import domain.auth.model.AuthUser
import domain.auth.repository.IAuthRepository
import domain.common.Try
import domain.common.fold
import domain.common.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

interface IAuthenticationService {
    fun loginWithGoogle(idToken: String): Flow<Try<AuthUser>>
    fun loginWithApple(idToken: String, fullName: String?, appleUserId: String): Flow<Try<AuthUser>>
    fun logout(): Flow<Try<Unit>>
    fun deleteAccount(): Flow<Try<Unit>>
}

class AuthenticationService(
    private val authRepository: IAuthRepository
) : IAuthenticationService {

    override fun loginWithGoogle(idToken: String): Flow<Try<AuthUser>> = flow {
        val result = authRepository.loginWithGoogle(idToken)
        result.fold(
            onSuccess = { user ->
                emit(Try.success(user))
            },
            onFailure = { error ->
                emit(Try.failure(Exception(error.message ?: "Login failed")))
            }
        )
    }

    override fun loginWithApple(idToken: String, fullName: String?, appleUserId: String): Flow<Try<AuthUser>> = flow {
        val result = authRepository.loginWithApple(idToken, fullName, appleUserId)
        result.fold(
            onSuccess = { user ->
                emit(Try.success(user))
            },
            onFailure = { error ->
                emit(Try.failure(Exception(error.message ?: "Login failed")))
            }
        )
    }

    override fun logout(): Flow<Try<Unit>> = flow {
        val result = authRepository.logout()
        result.fold(
            onSuccess = {
                emit(Try.success(Unit))
            },
            onFailure = { error ->
                emit(Try.failure(Exception(error.message ?: "Logout failed")))
            }
        )
    }

    override fun deleteAccount(): Flow<Try<Unit>> = flow {
        val result = authRepository.deleteAccount()
        result.fold(
            onSuccess = {
                emit(Try.success(Unit))
            },
            onFailure = { error ->
                emit(Try.failure(Exception(error.message ?: "Delete account failed")))
            }
        )
    }
}

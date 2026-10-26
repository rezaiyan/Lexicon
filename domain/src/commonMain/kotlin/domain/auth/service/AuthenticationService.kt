package domain.auth.service

import domain.auth.model.AuthUser
import domain.auth.repository.IAuthRepository
import core.common.getOrThrow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

interface IAuthenticationService {
    fun loginWithGoogle(idToken: String): Flow<AuthUser>
    fun loginWithApple(
        idToken: String,
        fullName: String?,
        appleUserId: String,
        authorizationCode: String? = null,
    ): Flow<AuthUser>
    fun logout(): Flow<Unit>
    fun deleteAccount(): Flow<Unit>
}

class AuthenticationService(
    private val authRepository: IAuthRepository
) : IAuthenticationService {

    override fun loginWithGoogle(idToken: String): Flow<AuthUser> = flow {
        emit(authRepository.loginWithGoogle(idToken).getOrThrow())
    }

    override fun loginWithApple(
        idToken: String,
        fullName: String?,
        appleUserId: String,
        authorizationCode: String?,
    ): Flow<AuthUser> = flow {
        emit(authRepository.loginWithApple(idToken, fullName, appleUserId, authorizationCode).getOrThrow())
    }

    override fun logout(): Flow<Unit> = flow {
        emit(authRepository.logout().getOrThrow())
    }

    override fun deleteAccount(): Flow<Unit> = flow {
        emit(authRepository.deleteAccount().getOrThrow())
    }
}

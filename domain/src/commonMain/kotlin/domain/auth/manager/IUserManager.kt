package domain.auth.manager

import domain.auth.model.AuthUser
import domain.common.Try
import kotlinx.coroutines.flow.Flow

interface IUserManager {
    fun isLogin(): Boolean
    fun observeUser(): Flow<AuthUser?>
    suspend fun loginWithGoogle(idToken: String): Try<AuthUser>
    suspend fun loginWithApple(idToken: String, fullName: String?, appleUserId: String): Try<AuthUser>
    suspend fun logout(): Try<Unit>
    suspend fun deleteAccount(): Try<Unit>
}



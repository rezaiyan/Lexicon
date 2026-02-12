package domain.auth.manager

import domain.auth.model.AuthUser
import kotlinx.coroutines.flow.Flow

interface IUserManager {
    fun isLogin(): Boolean
    fun observeUser(): Flow<AuthUser?>
    suspend fun loginWithGoogle(idToken: String): Result<AuthUser>
    suspend fun loginWithApple(idToken: String, fullName: String?, appleUserId: String): Result<AuthUser>
    suspend fun logout(): Result<Unit>
    suspend fun deleteAccount(): Result<Unit>
}


